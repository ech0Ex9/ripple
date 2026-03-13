package org.ripple.endpoint.core.git

import org.ripple.endpoint.detector.api.model.ChangeType
import org.ripple.endpoint.detector.api.model.ChangedFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

class DefaultGitChangeResolver : GitChangeResolver {
    
    override fun resolveWorkingChanges(projectPath: String): List<ChangedFile> {
        val result = mutableListOf<ChangedFile>()
        
        val statusOutput = runGitCommand(projectPath, "status", "--porcelain")
        
        statusOutput?.lines()?.forEach { line ->
            if (line.isBlank()) return@forEach
            
            val status = line.substring(0, 2).trim()
            val filePath = line.substring(3).trim()
            
            if (filePath.endsWith("/") || filePath == ".idea" || filePath.startsWith(".idea/")) {
                return@forEach
            }
            
            val changeType = when {
                status.contains("?") -> ChangeType.ADD
                status.contains("D") -> ChangeType.DELETE
                else -> ChangeType.MODIFY
            }
            
            if (changeType == ChangeType.MODIFY) {
                val hasRealChange = hasSubstantiveChanges(projectPath, filePath)
                if (!hasRealChange) return@forEach
            }
            
            val (added, deleted) = countChanges(projectPath, filePath, changeType)
            
            result.add(ChangedFile(
                path = filePath,
                changeType = changeType,
                linesAdded = added,
                linesDeleted = deleted
            ))
        }
        
        return result
    }
    
    override fun resolveStagedChanges(projectPath: String): List<ChangedFile> {
        val result = mutableListOf<ChangedFile>()
        
        runGitCommand(projectPath, "diff", "--cached", "--name-status")?.lines()?.forEach { line ->
            if (line.isBlank()) return@forEach
            
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val changeType = when (parts[0]) {
                    "A" -> ChangeType.ADD
                    "D" -> ChangeType.DELETE
                    else -> ChangeType.MODIFY
                }
                
                val filePath = parts[1]
                
                if (changeType == ChangeType.MODIFY) {
                    val hasRealChange = hasSubstantiveChangesStaged(projectPath, filePath)
                    if (!hasRealChange) return@forEach
                }
                
                result.add(ChangedFile(
                    path = filePath,
                    changeType = changeType
                ))
            }
        }
        
        return result
    }
    
    override fun resolveBranchDiff(
        projectPath: String,
        sourceBranch: String,
        targetBranch: String
    ): BranchDiffResult {
        val changes = mutableListOf<ChangedFile>()
        
        runGitCommand(projectPath, "diff", "--name-status", "$targetBranch...$sourceBranch")
            ?.lines()?.forEach { line ->
                if (line.isBlank()) return@forEach
                
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val changeType = when (parts[0]) {
                        "A" -> ChangeType.ADD
                        "D" -> ChangeType.DELETE
                        else -> ChangeType.MODIFY
                    }
                    
                    changes.add(ChangedFile(
                        path = parts[1],
                        changeType = changeType
                    ))
                }
            }
        
        val ahead = runGitCommand(projectPath, "rev-list", "--count", "$targetBranch..$sourceBranch")
            ?.trim()?.toIntOrNull() ?: 0
        
        val behind = runGitCommand(projectPath, "rev-list", "--count", "$sourceBranch..$targetBranch")
            ?.trim()?.toIntOrNull() ?: 0
        
        return BranchDiffResult(
            sourceBranch = sourceBranch,
            targetBranch = targetBranch,
            changes = changes,
            ahead = ahead,
            behind = behind
        )
    }
    
    override fun getCurrentBranch(projectPath: String): String? {
        return runGitCommand(projectPath, "branch", "--show-current")?.trim()
    }
    
    override fun getBranches(projectPath: String): List<String> {
        val branches = mutableListOf<String>()
        
        runGitCommand(projectPath, "branch", "-a", "--format=%(refname:short)")?.lines()?.forEach { 
            if (it.isNotBlank()) branches.add(it.trim())
        }
        
        return branches
    }
    
    private fun hasSubstantiveChanges(projectPath: String, filePath: String): Boolean {
        val diff = runGitCommand(projectPath, "diff", filePath)
        
        if (diff.isNullOrBlank()) {
            return true
        }
        
        return diff.lines().any { line ->
            line.startsWith("+") && !line.startsWith("+++") && isSubstantiveLine(line.substring(1))
        }
    }
    
    private fun hasSubstantiveChangesStaged(projectPath: String, filePath: String): Boolean {
        val diff = runGitCommand(projectPath, "diff", "--cached", filePath) ?: return true
        
        return diff.lines().any { line ->
            line.startsWith("+") && !line.startsWith("+++") && isSubstantiveLine(line.substring(1))
        }
    }
    
    private fun isSubstantiveLine(line: String): Boolean {
        val trimmed = line.trim()
        
        if (trimmed.isEmpty()) return false
        
        if (trimmed.startsWith("//")) return false
        if (trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.endsWith("*/")) return false
        if (trimmed.matches(Regex("^\\*.*"))) return false
        if (trimmed.startsWith("<!--") || trimmed.endsWith("-->")) return false
        if (trimmed.startsWith("#")) return false
        
        if (trimmed.startsWith("import ")) return false
        if (trimmed.startsWith("package ")) return false
        
        if (trimmed == "{" || trimmed == "}") return false
        
        return true
    }
    
    private fun countChanges(projectPath: String, filePath: String, changeType: ChangeType): Pair<Int, Int> {
        if (changeType == ChangeType.ADD) {
            val fullPath = Path.of(projectPath, filePath)
            return if (Files.exists(fullPath)) {
                val lines = Files.readAllLines(fullPath).count { isSubstantiveLine(it) }
                lines to 0
            } else 0 to 0
        }
        
        if (changeType == ChangeType.DELETE) {
            return 0 to 0
        }
        
        val diff = runGitCommand(projectPath, "diff", filePath) ?: return 0 to 0
        
        var added = 0
        var deleted = 0
        
        diff.lines().forEach { line ->
            when {
                line.startsWith("+") && !line.startsWith("+++") -> {
                    if (isSubstantiveLine(line.substring(1))) added++
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    if (isSubstantiveLine(line.substring(1))) deleted++
                }
            }
        }
        
        return added to deleted
    }
    
    private fun runGitCommand(workingDir: String, vararg args: String): String? {
        return try {
            val process = ProcessBuilder("git", *args)
                .directory(Path.of(workingDir).toFile())
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
}