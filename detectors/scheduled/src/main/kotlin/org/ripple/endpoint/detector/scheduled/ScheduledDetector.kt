package org.ripple.endpoint.detector.scheduled

import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.TrafficDetector
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.TrafficEntry

class ScheduledDetector : TrafficDetector {
    
    override val entryType = EntryType.SCHEDULED
    override val name = "Scheduled"
    override val description = "Detects Spring Scheduled tasks"
    
    override val defaultConfig = DetectorConfig(
        enabled = true,
        annotations = DEFAULT_ANNOTATIONS,
        basePackages = DEFAULT_PACKAGES
    )
    
    override fun detect(
        sourceCode: String,
        filePath: String,
        config: DetectorConfig
    ): List<TrafficEntry> {
        if (!filePath.endsWith(".java") && !filePath.endsWith(".kt")) {
            return emptyList()
        }
        
        val entries = mutableListOf<TrafficEntry>()
        
        // Find @Scheduled annotation with method
        val scheduledPattern = Regex(
            """@Scheduled\s*\(([^)]+)\)\s*(?:public\s+)?(?:void\s+)?(\w+)\s*\(""",
            RegexOption.DOT_MATCHES_ALL
        )
        
        scheduledPattern.findAll(sourceCode).forEach { match ->
            val annotationContent = match.groupValues[1]
            val methodName = match.groupValues[2]
            
            // Extract schedule configuration
            val cron = extractValue(annotationContent, "cron")
            val fixedRate = extractValue(annotationContent, "fixedRate")
            val fixedDelay = extractValue(annotationContent, "fixedDelay")
            val initialDelay = extractValue(annotationContent, "initialDelay")
            
            // Calculate line number
            val lineNumber = sourceCode.substring(0, match.range.first).lines().size
            
            val scheduleInfo = when {
                cron.isNotEmpty() -> "cron: $cron"
                fixedRate.isNotEmpty() -> "fixedRate: ${fixedRate}ms"
                fixedDelay.isNotEmpty() -> "fixedDelay: ${fixedDelay}ms"
                else -> "unknown schedule"
            }
            
            entries.add(TrafficEntry(
                type = EntryType.SCHEDULED,
                name = methodName,
                path = scheduleInfo,
                containingFile = filePath,
                line = lineNumber,
                annotations = DEFAULT_ANNOTATIONS,
                metadata = mapOf(
                    "cron" to cron,
                    "fixedRate" to fixedRate,
                    "fixedDelay" to fixedDelay,
                    "initialDelay" to initialDelay
                ).filterValues { it.isNotEmpty() }
            ))
        }
        
        return entries
    }
    
    override fun isApplicable(filePath: String, config: DetectorConfig): Boolean {
        val packages = config.basePackages.ifEmpty { DEFAULT_PACKAGES }
        return packages.any { pkg -> 
            filePath.contains("/$pkg/") || filePath.contains("\\$pkg\\")
        } && (filePath.endsWith(".java") || filePath.endsWith(".kt"))
    }
    
    private fun extractValue(content: String, key: String): String {
        val stringPattern = Regex("""$key\s*=\s*["']([^"']+)["']""")
        val numberPattern = Regex("""$key\s*=\s*(\d+)""")
        
        return stringPattern.find(content)?.groupValues?.get(1)
            ?: numberPattern.find(content)?.groupValues?.get(1)
            ?: ""
    }
    
    companion object {
        val DEFAULT_ANNOTATIONS = listOf(
            "org.springframework.scheduling.annotation.Scheduled"
        )
        val DEFAULT_PACKAGES = listOf("task", "job", "schedule", "cron")
    }
}