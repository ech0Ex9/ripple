package org.ripple.endpoint.core.engine

import org.ripple.endpoint.core.model.SensitivePattern
import org.ripple.endpoint.detector.api.model.ChangeType
import org.ripple.endpoint.detector.api.model.ChangedFile
import org.ripple.endpoint.detector.api.model.ImpactLevel
import org.ripple.endpoint.detector.api.model.TrafficEntry

class ImpactAnalyzer(
    private val sensitivePatterns: List<SensitivePattern> = emptyList()
) {
    
    fun analyze(
        entry: TrafficEntry,
        changedFile: ChangedFile,
        diff: FileDiff? = null
    ): ImpactLevel {
        return when {
            changedFile.changeType == ChangeType.DELETE -> ImpactLevel.HIGH
            
            diff?.hasSignatureChange == true -> ImpactLevel.HIGH
            
            isSensitiveEntry(entry) -> ImpactLevel.HIGH
            
            changedFile.changeType == ChangeType.ADD -> ImpactLevel.MEDIUM
            
            entry.containingFile == changedFile.path -> ImpactLevel.MEDIUM
            
            else -> ImpactLevel.LOW
        }
    }
    
    private fun isSensitiveEntry(entry: TrafficEntry): Boolean {
        val path = entry.path ?: return false
        return sensitivePatterns.any { pattern ->
            matchesPattern(pattern.pattern, path)
        }
    }
    
    private fun matchesPattern(pattern: String, path: String): Boolean {
        val regex = pattern
            .replace("**", ".*")
            .replace("*", "[^/]*")
            .toRegex()
        return regex.containsMatchIn(path)
    }
}

data class FileDiff(
    val hasSignatureChange: Boolean = false,
    val parametersAdded: Int = 0,
    val parametersRemoved: Int = 0,
    val returnTypeChanged: Boolean = false
)