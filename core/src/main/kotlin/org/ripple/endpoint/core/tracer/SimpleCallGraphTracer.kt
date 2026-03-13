package org.ripple.endpoint.core.tracer

import org.ripple.endpoint.core.model.CallEdge
import org.ripple.endpoint.core.model.CallGraph
import org.ripple.endpoint.core.model.CallGraphNode
import org.ripple.endpoint.core.model.CallRelation
import org.ripple.endpoint.core.model.DetectionResult
import org.ripple.endpoint.core.model.GraphMetadata
import org.ripple.endpoint.core.model.ImpactPath
import org.ripple.endpoint.core.model.NodeType
import org.ripple.endpoint.detector.api.model.ChangedFile
import org.ripple.endpoint.detector.api.model.ImpactLevel
import org.ripple.endpoint.detector.api.model.TrafficEntry
import java.time.Instant

class SimpleCallGraphTracer : CallGraphTracer, ReverseTracer {
    
    fun buildGraphFromResults(
        projectPath: String,
        results: List<DetectionResult>
    ): CallGraph {
        val nodes = mutableMapOf<String, CallGraphNode>()
        val edges = mutableListOf<CallEdge>()
        val entryPoints = mutableListOf<String>()
        
        results.forEach { result ->
            val entry = result.entry
            val entryNodeId = "${entry.containingFile}:${entry.name}"
            
            nodes[entryNodeId] = CallGraphNode(
                id = entryNodeId,
                type = NodeType.TRAFFIC_ENTRY,
                name = entry.name,
                qualifiedName = entry.name,
                filePath = entry.containingFile,
                line = entry.line,
                metadata = mapOf(
                    "type" to entry.type.name,
                    "path" to (entry.path ?: "")
                )
            )
            entryPoints.add(entryNodeId)
            
            var previousNodeId = entryNodeId
            
            result.callPath.forEach { callNode ->
                if (nodes[callNode.id] == null) {
                    nodes[callNode.id] = callNode
                }
                
                edges.add(CallEdge(
                    from = previousNodeId,
                    to = callNode.id,
                    relation = CallRelation.CALLS
                ))
                
                previousNodeId = callNode.id
            }
        }
        
        return CallGraph(
            nodes = nodes,
            edges = edges,
            entryPoints = entryPoints,
            changedNodes = emptySet(),
            metadata = GraphMetadata(
                project = projectPath,
                generatedAt = Instant.now().toString()
            )
        )
    }
    
    override fun traceFromEntry(
        entry: TrafficEntry,
        sourceCode: String,
        filePath: String,
        maxDepth: Int
    ): CallGraph {
        val nodes = mutableMapOf<String, CallGraphNode>()
        val edges = mutableListOf<CallEdge>()
        
        val entryNode = CallGraphNode(
            id = entry.id,
            type = NodeType.TRAFFIC_ENTRY,
            name = entry.name,
            qualifiedName = "${entry.containingFile}.${entry.name}",
            filePath = filePath,
            line = entry.line,
            metadata = mapOf("entryType" to entry.type.name)
        )
        nodes[entryNode.id] = entryNode
        
        val methodCalls = extractMethodCalls(sourceCode)
        methodCalls.forEach { call ->
            val callNode = CallGraphNode(
                id = "${filePath}:${call.methodName}",
                type = NodeType.METHOD,
                name = call.methodName,
                qualifiedName = call.qualifiedName,
                filePath = filePath,
                line = call.line
            )
            nodes[callNode.id] = callNode
            
            edges.add(CallEdge(
                from = entryNode.id,
                to = callNode.id,
                relation = CallRelation.CALLS
            ))
        }
        
        return CallGraph(
            nodes = nodes,
            edges = edges,
            entryPoints = listOf(entryNode.id),
            metadata = GraphMetadata(
                project = "",
                generatedAt = Instant.now().toString()
            )
        )
    }
    
    override fun traceFromEntries(
        entries: List<TrafficEntry>,
        sources: Map<String, String>,
        maxDepth: Int
    ): CallGraph {
        val allNodes = mutableMapOf<String, CallGraphNode>()
        val allEdges = mutableListOf<CallEdge>()
        val entryPointIds = mutableListOf<String>()
        
        for (entry in entries) {
            val sourceCode = sources[entry.containingFile] ?: continue
            val graph = traceFromEntry(entry, sourceCode, entry.containingFile, maxDepth)
            
            allNodes.putAll(graph.nodes)
            allEdges.addAll(graph.edges)
            entryPointIds.addAll(graph.entryPoints)
        }
        
        return CallGraph(
            nodes = allNodes,
            edges = allEdges,
            entryPoints = entryPointIds,
            metadata = GraphMetadata(
                project = "",
                generatedAt = Instant.now().toString()
            )
        )
    }
    
    override fun traceToEntries(
        changedFiles: List<ChangedFile>,
        allEntries: List<TrafficEntry>,
        callGraph: CallGraph
    ): List<ImpactPath> {
        val results = mutableListOf<ImpactPath>()
        
        val changedPaths = changedFiles.map { it.path }.toSet()
        
        for (entry in allEntries) {
            val entryNode = callGraph.nodes[entry.id] ?: continue
            
            val affectedNodes = callGraph.nodes.values.filter { node ->
                changedPaths.any { changedPath -> 
                    node.filePath.contains(changedPath) || changedPath.contains(node.filePath)
                }
            }
            
            if (affectedNodes.isNotEmpty()) {
                val path = findPath(callGraph, entryNode.id, affectedNodes.map { it.id })
                
                if (path.isNotEmpty()) {
                    results.add(ImpactPath(
                        entry = entry,
                        path = path,
                        impactLevel = determineImpactLevel(path)
                    ))
                }
            }
        }
        
        return results
    }
    
    private data class MethodCall(
        val methodName: String,
        val qualifiedName: String,
        val line: Int
    )
    
    private fun extractMethodCalls(sourceCode: String): List<MethodCall> {
        val calls = mutableListOf<MethodCall>()
        val lines = sourceCode.lines()
        
        val methodCallPattern = Regex("""(\w+)\s*\(""")
        
        lines.forEachIndexed { index, line ->
            methodCallPattern.findAll(line).forEach { match ->
                val methodName = match.groupValues[1]
                if (methodName !in listOf("if", "for", "while", "switch", "catch", "return", "new")) {
                    calls.add(MethodCall(
                        methodName = methodName,
                        qualifiedName = methodName,
                        line = index + 1
                    ))
                }
            }
        }
        
        return calls
    }
    
    private fun findPath(graph: CallGraph, from: String, toNodes: List<String>): List<CallGraphNode> {
        val path = mutableListOf<CallGraphNode>()
        val visited = mutableSetOf<String>()
        
        fun dfs(current: String): Boolean {
            if (current in visited) return false
            visited.add(current)
            
            val node = graph.nodes[current] ?: return false
            path.add(node)
            
            if (current in toNodes) return true
            
            for (edge in graph.edges) {
                if (edge.from == current) {
                    if (dfs(edge.to)) return true
                }
            }
            
            path.removeAt(path.size - 1)
            return false
        }
        
        dfs(from)
        return path
    }
    
    private fun determineImpactLevel(path: List<CallGraphNode>): ImpactLevel {
        return when {
            path.size <= 2 -> ImpactLevel.HIGH
            path.size <= 4 -> ImpactLevel.MEDIUM
            else -> ImpactLevel.LOW
        }
    }
}