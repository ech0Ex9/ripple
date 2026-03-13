package org.ripple.endpoint.detector.spring

import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.TrafficDetector
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.TrafficEntry
import kotlin.math.max

class SpringMvcDetector : TrafficDetector {
    
    override val entryType = EntryType.HTTP
    override val name = "Spring MVC"
    override val description = "Detects Spring MVC Controller HTTP endpoints"
    
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
        val annotations = config.annotations.ifEmpty { DEFAULT_ANNOTATIONS }
        
        // Extract class-level path
        val classPath = extractClassPath(sourceCode)
        val classDescription = extractClassDescription(sourceCode)
        
        // Find all methods with mapping annotations
        val lines = sourceCode.lines()
        for (i in lines.indices) {
            val line = lines[i]
            for (ann in annotations) {
                if (line.contains("@${ann.substringAfterLast('.')}") || 
                    line.contains("@${ann.substringAfterLast('.').removeSuffix("Mapping")}Mapping")) {
                    
                    val pathInfo = extractPathInfo(line, sourceCode, i)
                    val methodName = extractMethodName(sourceCode, i)
                    val methodDescription = extractMethodDescription(lines, i)
                    
                    if (methodName != null) {
                        val description = buildDescription(methodDescription, classDescription, methodName)
                        val fullPath = combinePath(classPath, pathInfo.path)
                        val httpMethod = pathInfo.method ?: "GET"
                        
                        entries.add(TrafficEntry(
                            type = EntryType.HTTP,
                            name = methodName,
                            path = "$httpMethod $fullPath",
                            description = description,
                            containingFile = filePath,
                            line = i + 1,
                            annotations = listOf(ann),
                            metadata = mapOf(
                                "httpMethod" to httpMethod,
                                "urlPath" to fullPath
                            )
                        ))
                    }
                }
            }
        }
        
        return entries.distinctBy { it.id }
    }
    
    override fun isApplicable(filePath: String, config: DetectorConfig): Boolean {
        val packages = config.basePackages.ifEmpty { DEFAULT_PACKAGES }
        return packages.any { pkg -> 
            filePath.contains("/$pkg/") || filePath.contains("\\$pkg\\")
        } && (filePath.endsWith(".java") || filePath.endsWith(".kt"))
    }
    
    private fun extractClassPath(sourceCode: String): String? {
        val classMapping = Regex(
            """@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']"""
        ).find(sourceCode)
        return classMapping?.groupValues?.get(1)
    }
    
    private data class PathInfo(val path: String, val method: String?)
    
    private fun extractPathInfo(line: String, sourceCode: String, lineIndex: Int): PathInfo {
        val pathPattern = Regex("""["']([^"']+)["']""")
        val path = pathPattern.find(line)?.groupValues?.get(1) ?: ""
        
        val method = when {
            line.contains("@GetMapping") -> "GET"
            line.contains("@PostMapping") -> "POST"
            line.contains("@PutMapping") -> "PUT"
            line.contains("@DeleteMapping") -> "DELETE"
            line.contains("@PatchMapping") -> "PATCH"
            else -> null
        }
        
        return PathInfo(path, method)
    }
    
    private fun extractMethodName(sourceCode: String, lineIndex: Int): String? {
        val lines = sourceCode.lines()
        if (lineIndex >= lines.size) return null
        
        // Look ahead for method signature
        for (i in lineIndex until minOf(lineIndex + 5, lines.size)) {
            val methodMatch = Regex("""(?:public|private|protected)?\s*\w+(?:<[^>]+>)?\s+(\w+)\s*\(""")
                .find(lines[i])
            if (methodMatch != null && methodMatch.groupValues[1] !in listOf("class", "interface", "if", "for", "while")) {
                return methodMatch.groupValues[1]
            }
        }
        return null
    }
    
    private fun combinePath(classPath: String?, methodPath: String): String {
        val base = classPath?.trim('/') ?: ""
        val method = methodPath.trim('/')
        return when {
            base.isEmpty() && method.isEmpty() -> "/"
            base.isEmpty() -> "/$method"
            method.isEmpty() -> "/$base"
            else -> "/$base/$method"
        }
    }
    
    private fun extractClassDescription(sourceCode: String): String? {
        // Find class-level JavaDoc
        val pattern = Regex("/\\*\\*(.*?)\\*/[\\s\\n]*public\\s+class", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(sourceCode)
        
        return match?.groups?.get(1)?.value?.let { doc ->
            doc.lines()
                .map { line -> line.trim().removePrefix("*").trim() }
                .filter { it.isNotEmpty() && !it.startsWith("@") }
                .joinToString(" ")
        }
    }
    
    private fun extractMethodDescription(lines: List<String>, startIndex: Int): String? {
        // Look backward for JavaDoc
        for (i in startIndex - 1 downTo maxOf(0, startIndex - 20)) {
            if (lines[i].contains("*/")) {
                // Found end of JavaDoc, now find the beginning
                for (j in i downTo maxOf(0, i - 20)) {
                    if (lines[j].contains("/**")) {
                        // Extract JavaDoc content
                        val docLines = mutableListOf<String>()
                        for (k in j + 1 until i) {
                            val line = lines[k].trim()
                            if (line.startsWith("*")) {
                                val content = line.removePrefix("*").trim()
                                if (content.isNotEmpty() && !content.startsWith("@")) {
                                    docLines.add(content)
                                }
                            }
                        }
                        return if (docLines.isNotEmpty()) docLines.joinToString(" ") else null
                    }
                }
                break
            }
        }
        return null
    }
    
    private fun buildDescription(methodDesc: String?, classDesc: String?, methodName: String): String {
        return when {
            !methodDesc.isNullOrBlank() -> methodDesc
            !classDesc.isNullOrBlank() -> "$classDesc - $methodName"
            else -> methodName
        }
    }
    
    companion object {
        val DEFAULT_ANNOTATIONS = listOf(
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping"
        )
        val DEFAULT_PACKAGES = listOf("controller", "api", "endpoint", "resource")
    }
}