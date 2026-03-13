package org.ripple.endpoint.core.graph

import org.ripple.endpoint.core.model.CallGraph
import org.ripple.endpoint.core.model.CallRelation
import org.ripple.endpoint.core.model.NodeType

class MermaidExporter : GraphExporter {
    
    override val format = GraphFormat.MERMAID
    override val fileExtension = "mmd"
    
    override fun export(graph: CallGraph): String {
        val sb = StringBuilder()
        
        sb.appendLine("flowchart TB")
        
        graph.nodes.values.forEach { node ->
            val nodeId = sanitizeId(node.id)
            val label = escapeLabel(node.name)
            val (openShape, closeShape) = nodeShape(node.type)
            val styleClass = nodeStyleClass(node.type, node.id in graph.changedNodes)
            
            sb.appendLine("    $nodeId$openShape$label$closeShape")
            if (styleClass != null) {
                sb.appendLine("    $nodeId::$styleClass")
            }
        }
        
        sb.appendLine()
        
        graph.edges.forEach { edge ->
            val fromId = sanitizeId(edge.from)
            val toId = sanitizeId(edge.to)
            val edgeStyle = edgeStyle(edge.relation)
            
            sb.appendLine("    $fromId $edgeStyle $toId")
        }
        
        sb.appendLine()
        sb.appendLine("    classDef trafficEntry fill:#ff6b6b,stroke:#c92a2a,stroke-width:2px,color:#fff")
        sb.appendLine("    classDef classNode fill:#4dabf7,stroke:#1971c2,color:#fff")
        sb.appendLine("    classDef methodNode fill:#ffd43b,stroke:#fab005,color:#000")
        sb.appendLine("    classDef fieldNode fill:#69db7c,stroke:#2f9e44,color:#fff")
        sb.appendLine("    classDef externalNode fill:#adb5bd,stroke:#868e96,stroke-dasharray: 5 5,color:#000")
        sb.appendLine("    classDef changed fill:#fff,stroke:#f03e3e,stroke-width:3px")
        
        return sb.toString()
    }
    
    private fun sanitizeId(id: String): String {
        return "n${id.hashCode().toString().replace("-", "m")}"
    }
    
    private fun escapeLabel(label: String): String {
        return label
            .replace("\"", "'")
            .replace("\n", " ")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
    
    private fun nodeShape(type: NodeType): Pair<String, String> {
        return when (type) {
            NodeType.TRAFFIC_ENTRY -> "([" to "])"
            NodeType.CLASS -> "[" to "]"
            NodeType.METHOD -> "[" to "]"
            NodeType.FIELD -> "[" to "]"
            NodeType.EXTERNAL -> "[" to "]"
        }
    }
    
    private fun nodeStyleClass(type: NodeType, isChanged: Boolean): String? {
        return when {
            isChanged -> "changed"
            type == NodeType.TRAFFIC_ENTRY -> "trafficEntry"
            type == NodeType.CLASS -> "classNode"
            type == NodeType.METHOD -> "methodNode"
            type == NodeType.FIELD -> "fieldNode"
            type == NodeType.EXTERNAL -> "externalNode"
            else -> null
        }
    }
    
    private fun edgeStyle(relation: CallRelation): String {
        return when (relation) {
            CallRelation.CALLS -> "-->"
            CallRelation.IMPLEMENTS -> "-.->"
            CallRelation.REFERENCES -> "-.->|ref|"
            CallRelation.ANNOTATED_BY -> "==>"
            CallRelation.HANDLES -> "-->|handles|"
            CallRelation.INVOKES -> "-->|invokes|"
        }
    }
}