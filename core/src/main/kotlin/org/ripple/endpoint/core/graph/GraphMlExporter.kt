package org.ripple.endpoint.core.graph

import org.ripple.endpoint.core.model.CallGraph

class GraphMlExporter : GraphExporter {
    
    override val format = GraphFormat.GRAPHML
    override val fileExtension = "graphml"
    
    override fun export(graph: CallGraph): String {
        val sb = StringBuilder()
        
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<graphml xmlns="http://graphml.graphdrawing.org/xmlns">""")
        
        // Key definitions
        sb.appendLine("""  <key id="type" for="node" attr.name="type" attr.type="string"/>""")
        sb.appendLine("""  <key id="name" for="node" attr.name="name" attr.type="string"/>""")
        sb.appendLine("""  <key id="filePath" for="node" attr.name="filePath" attr.type="string"/>""")
        sb.appendLine("""  <key id="line" for="node" attr.name="line" attr.type="int"/>""")
        sb.appendLine("""  <key id="relation" for="edge" attr.name="relation" attr.type="string"/>""")
        sb.appendLine("""  <key id="isEntry" for="node" attr.name="isEntry" attr.type="boolean"/>""")
        sb.appendLine("""  <key id="isChanged" for="node" attr.name="isChanged" attr.type="boolean"/>""")
        
        sb.appendLine("""  <graph id="G" edgedefault="directed">""")
        
        // Nodes
        graph.nodes.values.forEach { node ->
            sb.appendLine("""    <node id="${escapeXml(node.id)}">""")
            sb.appendLine("""      <data key="type">${node.type.name}</data>""")
            sb.appendLine("""      <data key="name">${escapeXml(node.name)}</data>""")
            sb.appendLine("""      <data key="filePath">${escapeXml(node.filePath)}</data>""")
            node.line?.let { sb.appendLine("""      <data key="line">$it</data>""") }
            sb.appendLine("""      <data key="isEntry">${node.id in graph.entryPoints}</data>""")
            sb.appendLine("""      <data key="isChanged">${node.id in graph.changedNodes}</data>""")
            sb.appendLine("""    </node>""")
        }
        
        // Edges
        graph.edges.forEach { edge ->
            sb.appendLine("""    <edge source="${escapeXml(edge.from)}" target="${escapeXml(edge.to)}">""")
            sb.appendLine("""      <data key="relation">${edge.relation.name}</data>""")
            sb.appendLine("""    </edge>""")
        }
        
        sb.appendLine("""  </graph>""")
        sb.appendLine("""</graphml>""")
        
        return sb.toString()
    }
    
    private fun escapeXml(s: String): String {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}