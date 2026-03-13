package org.ripple.endpoint.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeType {
    TRAFFIC_ENTRY,
    CLASS,
    METHOD,
    FIELD,
    EXTERNAL
}

@Serializable
enum class CallRelation {
    CALLS,
    IMPLEMENTS,
    REFERENCES,
    ANNOTATED_BY,
    HANDLES,
    INVOKES
}

@Serializable
data class CallGraphNode(
    val id: String,
    val type: NodeType,
    val name: String,
    val qualifiedName: String,
    val filePath: String,
    val line: Int? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class CallEdge(
    val from: String,
    val to: String,
    val relation: CallRelation,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class GraphMetadata(
    val project: String,
    val generatedAt: String,
    val sourceBranch: String? = null,
    val targetBranch: String? = null
)

@Serializable
data class CallGraph(
    val nodes: Map<String, CallGraphNode>,
    val edges: List<CallEdge>,
    val entryPoints: List<String>,
    val changedNodes: Set<String> = emptySet(),
    val metadata: GraphMetadata
)