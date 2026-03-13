package org.ripple.endpoint.core.graph

import org.ripple.endpoint.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class DotExporterTest {
    
    @Test
    fun `should export empty graph`() {
        val exporter = DotExporter()
        val graph = CallGraph(
            nodes = emptyMap(),
            edges = emptyList(),
            entryPoints = emptyList(),
            metadata = GraphMetadata("test", "2024-01-01")
        )
        
        val result = exporter.export(graph)
        
        assertTrue(result.contains("digraph CallGraph"))
        assertTrue(result.contains("rankdir=TB"))
    }
    
    @Test
    fun `should export nodes with correct styles`() {
        val exporter = DotExporter()
        val node = CallGraphNode(
            id = "test-id",
            type = NodeType.TRAFFIC_ENTRY,
            name = "testMethod",
            qualifiedName = "com.example.testMethod",
            filePath = "Test.java",
            line = 10
        )
        
        val graph = CallGraph(
            nodes = mapOf(node.id to node),
            edges = emptyList(),
            entryPoints = listOf(node.id),
            metadata = GraphMetadata("test", "2024-01-01")
        )
        
        val result = exporter.export(graph)
        
        assertTrue(result.contains("test-id"))
        assertTrue(result.contains("fillcolor=lightcoral"))
    }
    
    @Test
    fun `should export edges`() {
        val exporter = DotExporter()
        val node1 = CallGraphNode("id1", NodeType.TRAFFIC_ENTRY, "n1", "q1", "f1", 1)
        val node2 = CallGraphNode("id2", NodeType.METHOD, "n2", "q2", "f2", 2)
        val edge = CallEdge("id1", "id2", CallRelation.CALLS)
        
        val graph = CallGraph(
            nodes = mapOf("id1" to node1, "id2" to node2),
            edges = listOf(edge),
            entryPoints = listOf("id1"),
            metadata = GraphMetadata("test", "2024-01-01")
        )
        
        val result = exporter.export(graph)
        
        assertTrue(result.contains("\"id1\" -> \"id2\""))
    }
}

class JsonGraphExporterTest {
    
    @Test
    fun `should export valid JSON`() {
        val exporter = JsonGraphExporter()
        val graph = CallGraph(
            nodes = emptyMap(),
            edges = emptyList(),
            entryPoints = emptyList(),
            metadata = GraphMetadata("test", "2024-01-01")
        )
        
        val result = exporter.export(graph)
        
        assertTrue(result.contains("\"metadata\""))
        assertTrue(result.contains("\"nodes\""))
        assertTrue(result.contains("\"edges\""))
    }
}