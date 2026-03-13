package org.ripple.endpoint.core.graph

import org.ripple.endpoint.core.model.CallGraph
import org.ripple.endpoint.core.model.NodeType

class DotExporter : GraphExporter {
    
    override val format = GraphFormat.DOT
    override val fileExtension = "dot"
    
    override fun export(graph: CallGraph): String {
        val sb = StringBuilder()
        
        sb.appendLine("digraph CallGraph {")
        sb.appendLine("    rankdir=TB;")
        sb.appendLine("    node [shape=box, fontname=\"Arial\"];")
        sb.appendLine("    edge [fontname=\"Arial\"];")
        sb.appendLine()
        
        // Node styles by type
        sb.appendLine("    // Node definitions")
        graph.nodes.values.forEach { node ->
            val style = when (node.type) {
                NodeType.TRAFFIC_ENTRY -> "style=filled, fillcolor=lightcoral, color=red"
                NodeType.CLASS -> "style=filled, fillcolor=lightblue"
                NodeType.METHOD -> "style=filled, fillcolor=lightyellow"
                NodeType.FIELD -> "style=filled, fillcolor=lightgreen"
                NodeType.EXTERNAL -> "style=dashed, fillcolor=lightgray"
            }
            
            val highlight = if (node.id in graph.changedNodes) ", penwidth=2" else ""
            val label = node.name.replace("\"", "\\\"")
            
            sb.appendLine("    \"${escapeId(node.id)}\" [$style$highlight, label=\"$label\"];")
        }
        
        sb.appendLine()
        sb.appendLine("    // Edges")
        graph.edges.forEach { edge ->
            val style = when (edge.relation) {
                org.ripple.endpoint.core.model.CallRelation.CALLS -> "solid"
                org.ripple.endpoint.core.model.CallRelation.IMPLEMENTS -> "dashed"
                org.ripple.endpoint.core.model.CallRelation.REFERENCES -> "dotted"
                org.ripple.endpoint.core.model.CallRelation.ANNOTATED_BY -> "bold"
                else -> "solid"
            }
            
            sb.appendLine("    \"${escapeId(edge.from)}\" -> \"${escapeId(edge.to)}\" [style=$style];")
        }
        
        sb.appendLine("}")
        
        return sb.toString()
    }
    
    private fun escapeId(id: String): String {
        return id.replace("\"", "\\\"").replace("\n", " ")
    }
}