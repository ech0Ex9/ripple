package org.ripple.endpoint.core.graph

import org.ripple.endpoint.core.model.CallGraph
import java.nio.file.Path

enum class GraphFormat {
    DOT,
    GRAPHML,
    JSON,
    MERMAID  // Mermaid flowchart format
}

interface GraphExporter {
    
    val format: GraphFormat
    
    val fileExtension: String
    
    fun export(graph: CallGraph): String
    
    fun exportToFile(graph: CallGraph, outputPath: Path): Path {
        val content = export(graph)
        java.nio.file.Files.writeString(outputPath, content)
        return outputPath
    }
}