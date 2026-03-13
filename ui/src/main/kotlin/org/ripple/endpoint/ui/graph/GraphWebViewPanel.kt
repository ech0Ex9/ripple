package org.ripple.endpoint.ui.graph

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import org.ripple.endpoint.core.graph.MermaidExporter
import org.ripple.endpoint.core.model.CallGraph
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager

class GraphWebViewPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val browser: JBCefBrowser = JBCefBrowser()
    private val jsBridge: GraphJsBridge = GraphJsBridge(project)
    private var jsQuery: JBCefJSQuery? = null
    
    init {
        val jcefAvailable = try {
            browser.cefBrowser != null
        } catch (e: Exception) {
            false
        }
        
        if (jcefAvailable) {
            setupJsQuery()
            setupLoadHandler()
            add(browser.component, BorderLayout.CENTER)
            border = JBUI.Borders.empty()
            showEmptyState()
        } else {
            add(JBUI.Panels.simplePanel().apply {
                addToCenter(JLabel("Graph visualization requires JCEF support"))
            }, BorderLayout.CENTER)
        }
    }
    
    private fun setupJsQuery() {
        try {
            jsQuery = JBCefJSQuery.create(browser).apply {
                addHandler { query ->
                    if (query.startsWith("nodeClick:")) {
                        val nodeId = query.substringAfter("nodeClick:")
                        jsBridge.onNodeClick(nodeId)
                    }
                    null
                }
            }
        } catch (e: Exception) {
            jsQuery = null
        }
    }
    
    private fun setupLoadHandler() {
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    val nodeIdMapJson = jsBridge.getNodeIdMapJson()
                    executeJavaScript("setNodeIdMap($nodeIdMapJson)")
                    
                    val isDark = isDarkTheme()
                    executeJavaScript("setDarkTheme($isDark)")
                }
            }
        }, browser.cefBrowser)
    }
    
    fun loadGraph(graph: CallGraph) {
        jsBridge.setNodes(graph.nodes)
        
        val exporter = MermaidExporter()
        val mermaidContent = exporter.export(graph)
        
        val html = loadTemplate()
            .replace("<!--GRAPH_CONTENT-->", mermaidContent)
        
        val encoded = Base64.getEncoder().encodeToString(
            html.toByteArray(StandardCharsets.UTF_8)
        )
        browser.loadURL("data:text/html;base64,$encoded")
    }
    
    fun showEmptyState() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        height: 100vh;
                        margin: 0;
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <div>Run detection first, then click "查看图谱" to view the call graph</div>
            </body>
            </html>
        """.trimIndent()
        
        val encoded = Base64.getEncoder().encodeToString(
            html.toByteArray(StandardCharsets.UTF_8)
        )
        browser.loadURL("data:text/html;base64,$encoded")
    }
    
    private fun loadTemplate(): String {
        val stream = javaClass.getResourceAsStream("/graph-template.html")
            ?: return "<html><body>Template not found</body></html>"
        return String(stream.readAllBytes(), StandardCharsets.UTF_8)
    }
    
    private fun executeJavaScript(code: String) {
        ApplicationManager.getApplication().invokeLater {
            browser.cefBrowser?.executeJavaScript(code, browser.cefBrowser?.url, 0)
        }
    }
    
    private fun isDarkTheme(): Boolean {
        val lookAndFeel = UIManager.getLookAndFeel()
        return lookAndFeel?.name?.lowercase()?.contains("dark") == true
    }
    
    fun dispose() {
        browser.dispose()
    }
}