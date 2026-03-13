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
        
        for (entry in dedupedEntries) {
            val relatedChanges = request.changedFiles.filter { 
                isRelated(entry, it, request.projectPath) 
            }
            
            if (relatedChanges.isNotEmpty()) {
                val impactLevel = analyzeImpact(entry, relatedChanges)
                val sourceCode = readFile(request.projectPath, entry.containingFile)
                val impactReason = determineImpactReason(entry, relatedChanges, sourceCode)
                
                results.add(DetectionResult(
                    entry = entry,
                    impactLevel = impactLevel,
                    impactReason = impactReason,
                    changedFiles = relatedChanges.map { it.path }
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
    
    private fun readFile(projectPath: String, relativePath: String): String? {
        return try {
            val path = Path.of(projectPath, relativePath)
            if (Files.exists(path)) Files.readString(path) else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isRelated(entry: TrafficEntry, changedFile: ChangedFile, projectPath: String): Boolean {
        return entry.containingFile == changedFile.path
    }
    
    private fun analyzeImpact(entry: TrafficEntry, changedFiles: List<ChangedFile>): ImpactLevel {
        return when {
            changedFiles.any { it.changeType == ChangeType.DELETE } -> ImpactLevel.HIGH
            changedFiles.any { it.changeType == ChangeType.ADD } -> ImpactLevel.MEDIUM
            else -> ImpactLevel.MEDIUM
        }
    }
    
    private fun determineImpactReason(entry: TrafficEntry, changedFiles: List<ChangedFile>, sourceCode: String? = null): String {
        if (changedFiles.any { it.changeType == ChangeType.DELETE }) {
            return "入口文件被删除"
        }
        
        if (changedFiles.any { it.changeType == ChangeType.ADD }) {
            return "新增入口"
        }
        
        if (sourceCode != null) {
            val hasSignatureChange = analyzeMethodSignatureChanges(sourceCode, entry)
            if (hasSignatureChange != null) {
                return hasSignatureChange
            }
        }
        
        return "入口实现逻辑被修改"
    }
    
    private fun analyzeMethodSignatureChanges(sourceCode: String, entry: TrafficEntry): String? {
        val lines = sourceCode.lines()
        
        for ((index, line) in lines.withIndex()) {
            if (line.contains(entry.name) && line.contains("public")) {
                val signature = extractMethodSignature(line, lines, index)
                
                if (signature?.contains("(") == true && signature.contains(")")) {
                    val paramCount = signature.substringAfter("(").substringBefore(")").count { it == ',' } + 1
                    if (paramCount > 5) {
                        return "方法签名变更（参数过多）"
                    }
                    
                    if (signature.contains("Response") || signature.contains("Result")) {
                        return "返回类型可能变更"
                    }
                }
                break
            }
        }
        return null
    }
    
    private fun extractMethodSignature(startLine: String, allLines: List<String>, startIndex: Int): String? {
        var signature = startLine.trim()
        
        if (signature.endsWith("{") || signature.endsWith(";")) {
            return signature.substringBeforeLast('{').substringBeforeLast(';').trim()
        }
        
        for (i in startIndex + 1 until minOf(startIndex + 10, allLines.size)) {
            val line = allLines[i].trim()
            signature += " " + line
            
            if (line.endsWith("{") || line.endsWith(";")) {
                return signature.substringBeforeLast('{').substringBeforeLast(';').trim()
            }
        }
        
        return signature
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