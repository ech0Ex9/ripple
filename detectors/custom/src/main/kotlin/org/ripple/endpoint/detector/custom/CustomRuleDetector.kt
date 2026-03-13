package org.ripple.endpoint.detector.custom

import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.TrafficDetector
import org.ripple.endpoint.detector.api.model.CustomDetectorRule
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.NameExtractionType
import org.ripple.endpoint.detector.api.model.PathExtractionType
import org.ripple.endpoint.detector.api.model.TrafficEntry

class CustomRuleDetector(
    private val customRules: List<CustomDetectorRule> = emptyList()
) : TrafficDetector {
    
    override val entryType = EntryType.CUSTOM
    override val name = "Custom Rules"
    override val description = "用户自定义检测规则"
    
    override val defaultConfig = DetectorConfig()
    
    private val activeRules: List<CustomDetectorRule>
        get() = customRules.filter { it.enabled }
    
    override fun detect(
        sourceCode: String,
        filePath: String,
        config: DetectorConfig
    ): List<TrafficEntry> {
        val entries = mutableListOf<TrafficEntry>()
        
        for (rule in activeRules) {
            if (!matchesFilePattern(filePath, rule)) continue
            if (!matchesAnnotations(sourceCode, rule) && 
                !matchesClassNames(sourceCode, rule)) continue
            
            val foundEntries = extractEntries(sourceCode, filePath, rule)
            entries.addAll(foundEntries)
        }
        
        return entries
    }
    
    override fun isApplicable(filePath: String, config: DetectorConfig): Boolean {
        return activeRules.any { matchesFilePattern(filePath, it) }
    }
    
    private fun matchesFilePattern(filePath: String, rule: CustomDetectorRule): Boolean {
        if (rule.filePatterns.isEmpty()) return true
        
        return rule.filePatterns.any { pattern ->
            matchGlob(pattern, filePath)
        }
    }
    
    private fun matchesAnnotations(sourceCode: String, rule: CustomDetectorRule): Boolean {
        if (rule.annotations.isEmpty()) return false
        
        return rule.annotations.any { ann ->
            val shortName = ann.substringAfterLast('.')
            sourceCode.contains("@$shortName") || sourceCode.contains(ann)
        }
    }
    
    private fun matchesClassNames(sourceCode: String, rule: CustomDetectorRule): Boolean {
        if (rule.classNames.isEmpty()) return false
        
        return rule.classNames.any { pattern ->
            val classNamePattern = pattern
                .replace("*", "\\w+")
                .toRegex()
            
            val classMatch = Regex("""class\s+(\w+)""").findAll(sourceCode)
            classMatch.any { match ->
                classNamePattern.matches(match.groupValues[1])
            }
        }
    }
    
    private fun extractEntries(
        sourceCode: String,
        filePath: String,
        rule: CustomDetectorRule
    ): List<TrafficEntry> {
        val entries = mutableListOf<TrafficEntry>()
        val lines = sourceCode.lines()
        
        val matchedAnnotations = rule.annotations.filter { ann ->
            val shortName = ann.substringAfterLast('.')
            sourceCode.contains("@$shortName")
        }
        
        val matchedClassPatterns = rule.classNames.filter { pattern ->
            val classNamePattern = pattern.replace("*", "\\w+").toRegex()
            Regex("""class\s+(\w+)""").findAll(sourceCode)
                .any { classNamePattern.matches(it.groupValues[1]) }
        }
        
        for ((index, line) in lines.withIndex()) {
            val hasMatch = matchedAnnotations.any { ann ->
                val shortName = ann.substringAfterLast('.')
                line.contains("@$shortName")
            }
            
            if (hasMatch || (index == 0 && matchedClassPatterns.isNotEmpty())) {
                val name = extractName(sourceCode, index, rule)
                val path = extractPath(sourceCode, index, rule)
                val methodName = extractMethodName(lines, index)
                val lineNum = if (hasMatch) index + 1 else findClassLine(lines)
                
                entries.add(TrafficEntry(
                    type = rule.entryType,
                    name = name ?: methodName ?: rule.name,
                    path = path ?: "",
                    description = rule.description,
                    containingFile = filePath,
                    line = lineNum,
                    annotations = matchedAnnotations,
                    metadata = mapOf(
                        "ruleId" to rule.id,
                        "ruleName" to rule.name
                    )
                ))
            }
        }
        
        return entries.distinctBy { it.id }
    }
    
    private fun extractName(
        sourceCode: String,
        lineIndex: Int,
        rule: CustomDetectorRule
    ): String? {
        val extraction = rule.nameExtraction ?: return null
        val lines = sourceCode.lines()
        
        return when (extraction.type) {
            NameExtractionType.METHOD_NAME -> {
                extractMethodName(lines, lineIndex)
            }
            NameExtractionType.CLASS_NAME -> {
                Regex("""class\s+(\w+)""").find(sourceCode)?.groupValues?.get(1)
            }
            NameExtractionType.ANNOTATION_VALUE -> {
                if (extraction.pattern.isNotEmpty()) {
                    Regex(extraction.pattern).find(sourceCode)?.groupValues?.get(1)
                } else null
            }
            NameExtractionType.REGEX_GROUP -> {
                if (extraction.pattern.isNotEmpty()) {
                    Regex(extraction.pattern).find(sourceCode)?.groupValues?.get(1)
                } else null
            }
        }
    }
    
    private fun extractPath(
        sourceCode: String,
        lineIndex: Int,
        rule: CustomDetectorRule
    ): String? {
        val extraction = rule.pathExtraction ?: return null
        
        return when (extraction.type) {
            PathExtractionType.FROM_ANNOTATION -> {
                if (extraction.annotationAttr.isNotEmpty()) {
                    val pattern = """${extraction.annotationAttr}\s*=\s*["']([^"']+)["']"""
                    Regex(pattern).find(sourceCode)?.groupValues?.get(1)
                } else null
            }
            PathExtractionType.FROM_CLASS_NAME -> {
                val className = Regex("""class\s+(\w+)""").find(sourceCode)?.groupValues?.get(1)
                if (className != null && extraction.pattern.isNotEmpty()) {
                    val match = Regex(extraction.pattern).find(className)
                    match?.groupValues?.get(1) ?: extraction.defaultValue
                } else extraction.defaultValue.ifEmpty { "/${className ?: ""}" }
            }
            PathExtractionType.FROM_METHOD_NAME -> {
                val methodName = extractMethodName(sourceCode.lines(), lineIndex)
                methodName?.let { "/$it" }
            }
            PathExtractionType.FROM_REGEX -> {
                if (extraction.pattern.isNotEmpty()) {
                    Regex(extraction.pattern).find(sourceCode)?.groupValues?.get(1)
                } else null
            }
            PathExtractionType.FIXED_VALUE -> {
                extraction.defaultValue
            }
        }
    }
    
    private fun extractMethodName(lines: List<String>, startIndex: Int): String? {
        for (i in startIndex until minOf(startIndex + 10, lines.size)) {
            val methodMatch = Regex("""(?:public|private|protected)?\s*\w+(?:<[^>]+>)?\s+(\w+)\s*\(""")
                .find(lines[i])
            if (methodMatch != null && methodMatch.groupValues[1] !in listOf("class", "interface", "if", "for", "while")) {
                return methodMatch.groupValues[1]
            }
        }
        return null
    }
    
    private fun findClassLine(lines: List<String>): Int {
        for ((index, line) in lines.withIndex()) {
            if (line.contains("class ")) {
                return index + 1
            }
        }
        return 1
    }
    
    private fun matchGlob(pattern: String, path: String): Boolean {
        val regex = pattern
            .replace("**", ".*")
            .replace("*", "[^/]*")
            .replace("?", ".")
            .toRegex()
        
        return regex.containsMatchIn(path)
    }
}