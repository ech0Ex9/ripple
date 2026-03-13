package org.ripple.endpoint.ui.graph

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import org.ripple.endpoint.core.model.CallGraphNode

class GraphJsBridge(
    private val project: Project
) {
    private var nodeRegistry: Map<String, CallGraphNode> = emptyMap()
    
    fun setNodes(nodes: Map<String, CallGraphNode>) {
        this.nodeRegistry = nodes
    }
    
    @JvmOverloads
    fun onNodeClick(nodeId: String) {
        val node = nodeRegistry[nodeId] ?: return
        
        ApplicationManager.getApplication().invokeLater {
            navigateToNode(node)
        }
    }
    
    private fun navigateToNode(node: CallGraphNode) {
        if (node.filePath.isEmpty()) return
        
        val file = VirtualFileManager.getInstance()
            .findFileByUrl("file://${node.filePath}") ?: return
        
        val line = (node.line ?: 1) - 1
        
        PsiNavigationSupport.getInstance()
            .createNavigatable(project, file, line)
            .navigate(true)
    }
    
    fun getNodeIdMapJson(): String {
        val sb = StringBuilder("{")
        nodeRegistry.keys.forEachIndexed { index, id ->
            if (index > 0) sb.append(",")
            val sanitizedId = "n${id.hashCode().toString().replace("-", "m")}"
            sb.append("\"$sanitizedId\":\"${escapeJson(id)}\"")
        }
        sb.append("}")
        return sb.toString()
    }
    
    private fun escapeJson(s: String): String {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }
}