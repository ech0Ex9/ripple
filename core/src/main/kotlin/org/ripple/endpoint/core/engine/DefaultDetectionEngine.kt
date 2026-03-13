package org.ripple.endpoint.core.engine

import org.ripple.endpoint.core.model.CallGraph
import org.ripple.endpoint.core.model.DetectionResult
import org.ripple.endpoint.core.model.DetectionSummary
import org.ripple.endpoint.core.model.TypeStats
import org.ripple.endpoint.detector.api.TrafficDetector
import org.ripple.endpoint.detector.api.model.ChangeType
import org.ripple.endpoint.detector.api.model.ChangedFile
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.ImpactLevel
import org.ripple.endpoint.detector.api.model.TrafficEntry
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

class DefaultDetectionEngine(
    private val detectors: List<TrafficDetector>
) : DetectionEngine {
    
    override fun detect(request: DetectionRequest): DetectionResponse {
        val errors = mutableListOf<DetectionError>()
        val allEntries = mutableListOf<TrafficEntry>()
        val results = mutableListOf<DetectionResult>()
        
        for (changedFile in request.changedFiles) {
            if (changedFile.changeType == ChangeType.DELETE) {
                continue
            }
            
            val sourceCode = readFile(request.projectPath, changedFile.path)
            if (sourceCode == null) {
                errors.add(DetectionError(changedFile.path, "Cannot read file"))
                continue
            }
            
            for (detector in detectors) {
                val config = request.config[detector.name] ?: detector.defaultConfig
                if (!config.enabled) {
                    continue
                }
                
                val isApplicable = detector.isApplicable(changedFile.path, config)
                
                if (isApplicable) {
                    try {
                        val entries = detector.detect(sourceCode, changedFile.path, config)
                        allEntries.addAll(entries)
                    } catch (e: Exception) {
                        errors.add(DetectionError(changedFile.path, "Detection failed: ${e.message}", e))
                    }
                }
            }
        }
        
        val dedupedEntries = allEntries.distinctBy { it.id }
        
        val changedLinesByFile = mutableMapOf<String, Set<Int>>()
        
        for (entry in dedupedEntries) {
            val changedFile = request.changedFiles.find { it.path == entry.containingFile }
            
            if (changedFile == null) continue
            
            if (changedFile.changeType == ChangeType.ADD) {
                results.add(DetectionResult(
                    entry = entry,
                    impactLevel = ImpactLevel.MEDIUM,
                    impactReason = "新增入口",
                    changedFiles = listOf(changedFile.path)
                ))
                continue
            }
            
            if (changedFile.changeType == ChangeType.DELETE) {
                continue
            }
            
            if (!changedLinesByFile.containsKey(entry.containingFile)) {
                changedLinesByFile[entry.containingFile] = getChangedLines(
                    request.projectPath, 
                    entry.containingFile
                )
            }
            
            val changedLines = changedLinesByFile[entry.containingFile] ?: emptySet()
            
            val isAffected = isEntryAffected(entry, changedLines, request.projectPath)
            
            if (isAffected) {
                val impactLevel = analyzeImpact(entry, changedFile)
                val impactReason = determineImpactReason(entry, changedFile, changedLines)
                
                results.add(DetectionResult(
                    entry = entry,
                    impactLevel = impactLevel,
                    impactReason = impactReason,
                    changedFiles = listOf(changedFile.path)
                ))
            }
        }
        
        val summary = buildSummary(dedupedEntries, results)
        
        return DetectionResponse(
            results = results,
            callGraph = null,
            summary = summary,
            errors = errors
        )
    }
    
    private fun getChangedLines(projectPath: String, filePath: String): Set<Int> {
        val changedLines = mutableSetOf<Int>()
        
        try {
            val diffOutput = runGitDiff(projectPath, filePath) ?: return emptySet()
            
            var currentLine = 0
            
            for (line in diffOutput.lines()) {
                if (line.startsWith("@@")) {
                    val match = Regex("@@ -\\d+,?\\d* \\+(\\d+)").find(line)
                    if (match != null) {
                        currentLine = match.groupValues[1].toIntOrNull() ?: 0
                    }
                } else if (line.startsWith("+") && !line.startsWith("+++")) {
                    if (currentLine > 0) {
                        changedLines.add(currentLine)
                    }
                    currentLine++
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    // Don't increment currentLine for removed lines
                } else if (!line.startsWith("\\")) {
                    currentLine++
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return changedLines
    }
    
    private fun runGitDiff(projectPath: String, filePath: String): String? {
        return try {
            val process = ProcessBuilder("git", "diff", "-U0", filePath)
                .directory(Path.of(projectPath).toFile())
                .redirectErrorStream(true)
                .start()
            
            val output = BufferedReader(InputStreamReader(process.inputStream))
                .use { it.readText() }
            
            val exitCode = process.waitFor()
            if (exitCode == 0) output else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isEntryAffected(entry: TrafficEntry, changedLines: Set<Int>, projectPath: String): Boolean {
        if (changedLines.isEmpty()) return false
        
        val entryLine = entry.line
        if (entryLine <= 0) return false
        
        if (changedLines.contains(entryLine)) return true
        
        val sourceCode = readFile(projectPath, entry.containingFile) ?: return false
        val lines = sourceCode.lines()
        
        val methodStartLine = entryLine - 1
        val methodEndLine = findMethodEndLine(lines, methodStartLine)
        
        for (lineNum in changedLines) {
            if (lineNum >= entryLine && lineNum <= methodEndLine) {
                return true
            }
        }
        
        return false
    }
    
    private fun findMethodEndLine(lines: List<String>, startLine: Int): Int {
        var braceCount = 0
        var foundOpenBrace = false
        
        for (i in startLine until lines.size) {
            val line = lines[i]
            
            for (char in line) {
                when (char) {
                    '{' -> {
                        braceCount++
                        foundOpenBrace = true
                    }
                    '}' -> {
                        braceCount--
                        if (foundOpenBrace && braceCount == 0) {
                            return i + 1
                        }
                    }
                }
            }
        }
        
        return minOf(startLine + 50, lines.size)
    }
    
    private fun readFile(projectPath: String, relativePath: String): String? {
        return try {
            val path = Path.of(projectPath, relativePath)
            if (Files.exists(path)) Files.readString(path) else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun analyzeImpact(entry: TrafficEntry, changedFile: ChangedFile): ImpactLevel {
        return when {
            changedFile.changeType == ChangeType.DELETE -> ImpactLevel.HIGH
            changedFile.changeType == ChangeType.ADD -> ImpactLevel.MEDIUM
            else -> ImpactLevel.MEDIUM
        }
    }
    
    private fun determineImpactReason(entry: TrafficEntry, changedFile: ChangedFile, changedLines: Set<Int>): String {
        if (changedFile.changeType == ChangeType.ADD) {
            return "新增入口"
        }
        
        val entryLine = entry.line
        
        return if (changedLines.contains(entryLine)) {
            "入口定义被修改"
        } else {
            "入口实现逻辑被修改"
        }
    }
    
    private fun buildSummary(allEntries: List<TrafficEntry>, results: List<DetectionResult>): DetectionSummary {
        val byType = allEntries.groupBy { it.type }.mapValues { (type, entries) ->
            val affected = results.count { it.entry.type == type }
            val highRisk = results.count { it.entry.type == type && it.impactLevel == ImpactLevel.HIGH }
            TypeStats(total = entries.size, affected = affected, highRisk = highRisk)
        }
        
        val byImpact = results.groupBy { it.impactLevel }.mapValues { it.value.size }
        
        return DetectionSummary(
            totalEntries = allEntries.size,
            affectedEntries = results.size,
            byType = byType.mapKeys { it.key.name },
            byImpact = byImpact.withDefault { 0 }
        )
    }
}

interface DetectionEngine {
    fun detect(request: DetectionRequest): DetectionResponse
}