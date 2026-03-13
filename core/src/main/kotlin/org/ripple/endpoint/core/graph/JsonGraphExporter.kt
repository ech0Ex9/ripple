package org.ripple.endpoint.core.graph

import org.ripple.endpoint.core.model.CallGraph
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonGraphExporter : GraphExporter {
    
    private val json = Json { 
        prettyPrint = true
        encodeDefaults = true
    }
    
    override val format = GraphFormat.JSON
    override val fileExtension = "json"
    
    override fun export(graph: CallGraph): String {
        val dto = CallGraphDto(
            metadata = GraphMetadataDto(
                project = graph.metadata.project,
                generatedAt = graph.metadata.generatedAt,
                sourceBranch = graph.metadata.sourceBranch,
                targetBranch = graph.metadata.targetBranch,
                totalNodes = graph.nodes.size,
                totalEdges = graph.edges.size,
                entryPoints = graph.entryPoints.size
            ),
            nodes = graph.nodes.values.map { node ->
                NodeDto(
                    id = node.id,
                    type = node.type.name,
                    name = node.name,
                    qualifiedName = node.qualifiedName,
                    filePath = node.filePath,
                    line = node.line,
                    metadata = node.metadata,
                    isChanged = node.id in graph.changedNodes,
                    isEntryPoint = node.id in graph.entryPoints
                )
            },
            edges = graph.edges.map { edge ->
                EdgeDto(
                    from = edge.from,
                    to = edge.to,
                    relation = edge.relation.name,
                    metadata = edge.metadata
                )
            },
            entryPoints = graph.entryPoints,
            changedNodes = graph.changedNodes.toList()
        )
        
        return json.encodeToString(dto)
    }
}

@kotlinx.serialization.Serializable
data class CallGraphDto(
    val metadata: GraphMetadataDto,
    val nodes: List<NodeDto>,
    val edges: List<EdgeDto>,
    val entryPoints: List<String>,
    val changedNodes: List<String>
)

@kotlinx.serialization.Serializable
data class GraphMetadataDto(
    val project: String,
    val generatedAt: String,
    val sourceBranch: String? = null,
    val targetBranch: String? = null,
    val totalNodes: Int,
    val totalEdges: Int,
    val entryPoints: Int
)

@kotlinx.serialization.Serializable
data class NodeDto(
    val id: String,
    val type: String,
    val name: String,
    val qualifiedName: String,
    val filePath: String,
    val line: Int?,
    val metadata: Map<String, String>,
    val isChanged: Boolean,
    val isEntryPoint: Boolean
)

@kotlinx.serialization.Serializable
data class EdgeDto(
    val from: String,
    val to: String,
    val relation: String,
    val metadata: Map<String, String>
)