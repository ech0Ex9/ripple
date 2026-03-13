# Graph Visualization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add interactive call graph visualization using JCEF WebView + Mermaid.js with zoom, pan, search, and click-to-navigate.

**Architecture:** Extend existing GraphExporter system with MermaidExporter. Create GraphWebViewPanel using JCEF browser. Modify ResultTreePanel to toggle between tree and graph views. Java-JS bridge for node click navigation.

**Tech Stack:** Kotlin, IntelliJ Platform SDK (JCEF), Mermaid.js, HTML/CSS/JavaScript

---

## Task 1: Add MERMAID to GraphFormat Enum

**Files:**
- Modify: `core/src/main/kotlin/com/xxx/endpoint/core/graph/GraphExporter.kt`

**Step 1: Add MERMAID to enum**

```kotlin
enum class GraphFormat {
    DOT,
    GRAPHML,
    JSON,
    MERMAID  // New: Mermaid flowchart format
}
```

**Step 2: Verify syntax**

Run: `cd /Users/eric/development/idea-endpoint-plugin && ./gradlew :core:compileKotlin --quiet`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add core/src/main/kotlin/com/xxx/endpoint/core/graph/GraphExporter.kt
git commit -m "feat(core): add MERMAID to GraphFormat enum"
```

---

## Task 2: Implement MermaidExporter

**Files:**
- Create: `core/src/main/kotlin/com/xxx/endpoint/core/graph/MermaidExporter.kt`

**Step 1: Create MermaidExporter class**

```kotlin
package com.xxx.endpoint.core.graph

import com.xxx.endpoint.core.model.CallGraph
import com.xxx.endpoint.core.model.CallRelation
import com.xxx.endpoint.core.model.NodeType

class MermaidExporter : GraphExporter {
    
    override val format = GraphFormat.MERMAID
    override val fileExtension = "mmd"
    
    override fun export(graph: CallGraph): String {
        val sb = StringBuilder()
        
        sb.appendLine("```mermaid")
        sb.appendLine("flowchart TB")
        
        // Node definitions with styling
        graph.nodes.values.forEach { node ->
            val nodeId = sanitizeId(node.id)
            val label = escapeLabel(node.name)
            val shape = nodeShape(node.type)
            val styleClass = nodeStyleClass(node.type, node.id in graph.changedNodes)
            
            sb.appendLine("    $nodeId$shape$label$shape")
            if (styleClass != null) {
                sb.appendLine("    $nodeId::$styleClass")
            }
        }
        
        sb.appendLine()
        
        // Edge definitions
        graph.edges.forEach { edge ->
            val fromId = sanitizeId(edge.from)
            val toId = sanitizeId(edge.to)
            val edgeStyle = edgeStyle(edge.relation)
            
            sb.appendLine("    $fromId $edgeStyle $toId")
        }
        
        // Style class definitions
        sb.appendLine()
        sb.appendLine("    classDef trafficEntry fill:#ff6b6b,stroke:#c92a2a,stroke-width:2px,color:#fff")
        sb.appendLine("    classDef classNode fill:#4dabf7,stroke:#1971c2,color:#fff")
        sb.appendLine("    classDef methodNode fill:#ffd43b,stroke:#fab005,color:#000")
        sb.appendLine("    classDef fieldNode fill:#69db7c,stroke:#2f9e44,color:#fff")
        sb.appendLine("    classDef externalNode fill:#adb5bd,stroke:#868e96,stroke-dasharray: 5 5,color:#000")
        sb.appendLine("    classDef changed fill:#fff,stroke:#f03e3e,stroke-width:3px")
        
        sb.appendLine("```")
        
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
    
    private fun nodeShape(type: NodeType): String {
        return when (type) {
            NodeType.TRAFFIC_ENTRY -> "(["
            NodeType.CLASS -> "["
            NodeType.METHOD -> "["
            NodeType.FIELD -> "["
            NodeType.EXTERNAL -> "["
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
            CallRelation.HANDLES --> "-->|handles|"
            CallRelation.INVOKES -> "-->|invokes|"
        }
    }
}
```

**Step 2: Verify compilation**

Run: `cd /Users/eric/development/idea-endpoint-plugin && ./gradlew :core:compileKotlin --quiet`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add core/src/main/kotlin/com/xxx/endpoint/core/graph/MermaidExporter.kt
git commit -m "feat(core): add MermaidExporter for call graph visualization"
```

---

## Task 3: Create Graph HTML Template

**Files:**
- Create: `ui/src/main/resources/graph-template.html`

**Step 1: Create HTML template**

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: var(--bg-color, #ffffff);
            color: var(--text-color, #333333);
            overflow: hidden;
        }
        
        body.dark {
            --bg-color: #2b2b2b;
            --text-color: #a9b7c6;
            --border-color: #3c3f41;
            --input-bg: #3c3f41;
        }
        
        .toolbar {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            height: 40px;
            background: var(--bg-color);
            border-bottom: 1px solid var(--border-color, #e0e0e0);
            display: flex;
            align-items: center;
            padding: 0 10px;
            gap: 10px;
            z-index: 100;
        }
        
        .search-box {
            flex: 1;
            max-width: 300px;
            padding: 6px 10px;
            border: 1px solid var(--border-color, #e0e0e0);
            border-radius: 4px;
            background: var(--input-bg, #ffffff);
            color: var(--text-color);
            font-size: 13px;
        }
        
        .search-box:focus {
            outline: none;
            border-color: #4dabf7;
        }
        
        .zoom-controls {
            display: flex;
            gap: 5px;
        }
        
        .zoom-btn {
            width: 28px;
            height: 28px;
            border: 1px solid var(--border-color, #e0e0e0);
            border-radius: 4px;
            background: var(--input-bg, #ffffff);
            color: var(--text-color);
            cursor: pointer;
            font-size: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .zoom-btn:hover {
            background: #4dabf7;
            color: white;
        }
        
        .zoom-level {
            font-size: 12px;
            color: var(--text-color);
            min-width: 50px;
            text-align: center;
        }
        
        .graph-container {
            position: absolute;
            top: 40px;
            left: 0;
            right: 0;
            bottom: 0;
            overflow: hidden;
            cursor: grab;
        }
        
        .graph-container:active {
            cursor: grabbing;
        }
        
        .graph-wrapper {
            transform-origin: top left;
            padding: 20px;
        }
        
        .mermaid {
            display: flex;
            justify-content: center;
        }
        
        .node {
            cursor: pointer;
            transition: opacity 0.2s;
        }
        
        .node:hover {
            opacity: 0.8;
        }
        
        .node.dimmed {
            opacity: 0.3;
        }
        
        .node.highlighted {
            opacity: 1;
        }
        
        .stats {
            font-size: 11px;
            color: var(--text-color);
            opacity: 0.7;
        }
        
        .empty-message {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100%;
            color: var(--text-color);
            opacity: 0.5;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="toolbar">
        <input type="text" class="search-box" id="searchBox" placeholder="Search nodes...">
        <div class="zoom-controls">
            <button class="zoom-btn" onclick="zoomIn()">+</button>
            <button class="zoom-btn" onclick="zoomOut()">−</button>
            <button class="zoom-btn" onclick="resetView()">⟲</button>
        </div>
        <span class="zoom-level" id="zoomLevel">100%</span>
        <span class="stats" id="stats"></span>
    </div>
    
    <div class="graph-container" id="graphContainer">
        <div class="graph-wrapper" id="graphWrapper">
            <!--GRAPH_CONTENT-->
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <script>
        // Configuration
        let scale = 1;
        let panX = 0;
        let panY = 0;
        let isDragging = false;
        let startX, startY;
        let nodeIdMap = {};
        
        const container = document.getElementById('graphContainer');
        const wrapper = document.getElementById('graphWrapper');
        
        // Initialize Mermaid
        mermaid.initialize({
            startOnLoad: true,
            theme: document.body.classList.contains('dark') ? 'dark' : 'default',
            flowchart: {
                curve: 'basis',
                padding: 15
            },
            securityLevel: 'loose'
        });
        
        // After render, set up node click handlers
        mermaid.run().then(() => {
            setupNodeClickHandlers();
            updateStats();
        });
        
        function setupNodeClickHandlers() {
            document.querySelectorAll('.node').forEach(node => {
                node.addEventListener('click', function(e) {
                    e.stopPropagation();
                    const nodeId = this.id;
                    if (window.jsBridge && nodeIdMap[nodeId]) {
                        window.jsBridge.onNodeClick(nodeIdMap[nodeId]);
                    }
                });
            });
        }
        
        function updateStats() {
            const nodeCount = document.querySelectorAll('.node').length;
            const edgeCount = document.querySelectorAll('.edge-path').length;
            document.getElementById('stats').textContent = `${nodeCount} nodes, ${edgeCount} edges`;
        }
        
        // Zoom functions
        function zoomIn() {
            scale = Math.min(scale + 0.2, 3);
            applyTransform();
        }
        
        function zoomOut() {
            scale = Math.max(scale - 0.2, 0.3);
            applyTransform();
        }
        
        function resetView() {
            scale = 1;
            panX = 0;
            panY = 0;
            applyTransform();
        }
        
        function applyTransform() {
            wrapper.style.transform = `translate(${panX}px, ${panY}px) scale(${scale})`;
            document.getElementById('zoomLevel').textContent = Math.round(scale * 100) + '%';
        }
        
        // Pan handlers
        container.addEventListener('mousedown', (e) => {
            if (e.target === container || e.target === wrapper) {
                isDragging = true;
                startX = e.clientX - panX;
                startY = e.clientY - panY;
            }
        });
        
        document.addEventListener('mousemove', (e) => {
            if (isDragging) {
                panX = e.clientX - startX;
                panY = e.clientY - startY;
                applyTransform();
            }
        });
        
        document.addEventListener('mouseup', () => {
            isDragging = false;
        });
        
        // Mouse wheel zoom
        container.addEventListener('wheel', (e) => {
            e.preventDefault();
            const delta = e.deltaY > 0 ? -0.1 : 0.1;
            scale = Math.max(0.3, Math.min(3, scale + delta));
            applyTransform();
        });
        
        // Search/filter
        document.getElementById('searchBox').addEventListener('input', function() {
            const query = this.value.toLowerCase().trim();
            document.querySelectorAll('.node').forEach(node => {
                const text = node.textContent.toLowerCase();
                if (query === '' || text.includes(query)) {
                    node.classList.remove('dimmed');
                    node.classList.add('highlighted');
                } else {
                    node.classList.add('dimmed');
                    node.classList.remove('highlighted');
                }
            });
        });
        
        // Set node ID mapping (called from Java)
        function setNodeIdMap(map) {
            nodeIdMap = map;
        }
        
        // Set dark theme (called from Java)
        function setDarkTheme(isDark) {
            if (isDark) {
                document.body.classList.add('dark');
            } else {
                document.body.classList.remove('dark');
            }
        }
    </script>
</body>
</html>
```

**Step 2: Verify file created**

Run: `ls -la /Users/eric/development/idea-endpoint-plugin/ui/src/main/resources/graph-template.html`

Expected: File exists

**Step 3: Commit**

```bash
git add ui/src/main/resources/graph-template.html
git commit -m "feat(ui): add HTML template for graph visualization"
```

---

## Task 4: Implement GraphJsBridge

**Files:**
- Create: `ui/src/main/kotlin/com/xxx/endpoint/ui/graph/GraphJsBridge.kt`

**Step 1: Create GraphJsBridge class**

```kotlin
package com.xxx.endpoint.ui.graph

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.xxx.endpoint.core.model.CallGraphNode

/**
 * JavaScript bridge for handling events from the graph WebView.
 * Enables node click navigation to source files.
 */
class GraphJsBridge(
    private val project: Project
) {
    private var nodeRegistry: Map<String, CallGraphNode> = emptyMap()
    
    /**
     * Update the node registry with current graph data.
     * Must be called before the graph is rendered.
     */
    fun setNodes(nodes: Map<String, CallGraphNode>) {
        this.nodeRegistry = nodes
    }
    
    /**
     * Called from JavaScript when user clicks a node.
     * Navigates to the source file at the node's line.
     */
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
        
        val line = (node.line ?: 1) - 1 // Convert to 0-indexed
        
        PsiNavigationSupport.getInstance()
            .createNavigatable(project, file, line)
            .navigate(true)
    }
    
    /**
     * Get the node ID map as JSON for JavaScript.
     * Maps sanitized IDs to original IDs.
     */
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
```

**Step 2: Verify compilation**

Run: `cd /Users/eric/development/idea-endpoint-plugin && ./gradlew :ui:compileKotlin --quiet`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add ui/src/main/kotlin/com/xxx/endpoint/ui/graph/GraphJsBridge.kt
git commit -m "feat(ui): add GraphJsBridge for node click navigation"
```

---

## Task 5: Implement GraphWebViewPanel

**Files:**
- Create: `ui/src/main/kotlin/com/xxx/endpoint/ui/graph/GraphWebViewPanel.kt`

**Step 1: Create GraphWebViewPanel class**

```kotlin
package com.xxx.endpoint.ui.graph

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import com.xxx.endpoint.core.graph.MermaidExporter
import com.xxx.endpoint.core.model.CallGraph
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.swing.JPanel

/**
 * Panel containing JCEF browser for rendering call graphs.
 * Uses Mermaid.js for flowchart visualization.
 */
class GraphWebViewPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val browser: JBCefBrowser
    private val jsBridge: GraphJsBridge
    private var jsQuery: JBCefJSQuery? = null
    
    init {
        // Check JCEF availability
        if (!JBCefBrowser.isJBCefEnabled()) {
            add(JBUI.Panels.simplePanel().apply {
                addToCenter(javax.swing.JLabel("Graph visualization requires JCEF support"))
            }, BorderLayout.CENTER)
            browser = JBCefBrowser()
            jsBridge = GraphJsBridge(project)
            return
        }
        
        browser = JBCefBrowser()
        jsBridge = GraphJsBridge(project)
        
        setupJsQuery()
        setupLoadHandler()
        
        add(browser.component, BorderLayout.CENTER)
        border = JBUI.Borders.empty()
        
        showEmptyState()
    }
    
    private fun setupJsQuery() {
        jsQuery = JBCefJSQuery.create(browser as JBCefBrowser.Base).apply {
            addHandler { query ->
                // Handle queries from JavaScript
                if (query.startsWith("nodeClick:")) {
                    val nodeId = query.substringAfter("nodeClick:")
                    jsBridge.onNodeClick(nodeId)
                }
                null
            }
        }
    }
    
    private fun setupLoadHandler() {
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    // Inject the node ID map after page loads
                    val nodeIdMapJson = jsBridge.getNodeIdMapJson()
                    executeJavaScript("setNodeIdMap($nodeIdMapJson)")
                    
                    // Set theme
                    val isDark = LafManager.getInstance().currentLookAndFeel?.isDark ?: false
                    executeJavaScript("setDarkTheme($isDark)")
                }
            }
        }, browser.cefBrowser)
    }
    
    /**
     * Load and render a call graph.
     */
    fun loadGraph(graph: CallGraph) {
        jsBridge.setNodes(graph.nodes)
        
        val exporter = MermaidExporter()
        val mermaidContent = exporter.export(graph)
        
        // Load template and inject content
        val html = loadTemplate()
            .replace("<!--GRAPH_CONTENT-->", mermaidContent)
        
        // Use base64 encoding to avoid issues with special characters
        val encoded = Base64.getEncoder().encodeToString(
            html.toByteArray(StandardCharsets.UTF_8)
        )
        browser.loadURL("data:text/html;base64,$encoded")
    }
    
    /**
     * Show empty state message.
     */
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
    
    /**
     * Dispose resources.
     */
    fun dispose() {
        browser.dispose()
    }
}
```

**Step 2: Verify compilation**

Run: `cd /Users/eric/development/idea-endpoint-plugin && ./gradlew :ui:compileKotlin --quiet`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add ui/src/main/kotlin/com/xxx/endpoint/ui/graph/GraphWebViewPanel.kt
git commit -m "feat(ui): add GraphWebViewPanel with JCEF browser for graph rendering"
```

---

## Task 6: Modify ResultTreePanel for Graph Toggle

**Files:**
- Modify: `ui/src/main/kotlin/com/xxx/endpoint/ui/toolwindow/ResultTreePanel.kt`

**Step 1: Update imports and add graph panel**

Add to imports:
```kotlin
import com.xxx.endpoint.core.graph.MermaidExporter
import com.xxx.endpoint.core.model.CallGraph
import com.xxx.endpoint.core.tracer.SimpleCallGraphTracer
import com.xxx.endpoint.ui.graph.GraphWebViewPanel
import java.awt.CardLayout
```

**Step 2: Add CardLayout and graph panel fields**

Replace the class initialization section:

```kotlin
class ResultTreePanel(private val project: Project) : JPanel(CardLayout()) {
    
    private val treePanel: JPanel
    private val graphPanel: GraphWebViewPanel
    private val cardLayout: CardLayout = layout as CardLayout
    
    private val tree: Tree
    private val rootNode = DefaultMutableTreeNode("点击「检测变更」开始")
    private val model = DefaultTreeModel(rootNode)
    private val summaryLabel = JBLabel(" ")
    
    private var currentResults: List<DetectionResult> = emptyList()
    private var currentSummary: DetectionSummary? = null
    private var currentGraph: CallGraph? = null
    private var showingGraph = false
    
    private val showGraphButton: JButton
    private val backButton: JButton
    
    init {
        // Tree panel setup
        tree = Tree(model).apply {
            isRootVisible = true
            cellRenderer = ResultTreeCellRenderer()
        }
        
        // Buttons
        showGraphButton = JButton("查看图谱").apply {
            addActionListener { showGraph() }
            isEnabled = false
        }
        
        backButton = JButton("返回列表").apply {
            addActionListener { showTree() }
            isVisible = false
        }
        
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("检测变更").apply {
                addActionListener { performDetection() }
            })
            add(JButton("生成报告").apply {
                addActionListener { generateReport() }
            })
            add(showGraphButton)
            add(backButton)
        }
        
        // Tree panel
        treePanel = JPanel(BorderLayout()).apply {
            add(toolbar, BorderLayout.NORTH)
            add(JBScrollPane(tree), BorderLayout.CENTER)
            add(summaryLabel.apply { border = JBUI.Borders.empty(5) }, BorderLayout.SOUTH)
            border = JBUI.Borders.empty(10)
        }
        
        // Graph panel
        graphPanel = GraphWebViewPanel(project)
        
        add(treePanel, "tree")
        add(graphPanel, "graph")
    }
```

**Step 3: Add showGraph and showTree methods**

Add these methods to the class:

```kotlin
    private fun showGraph() {
        val graph = currentGraph
        if (graph == null || graph.nodes.isEmpty()) {
            EndpointNotifier.notifyError(project, "No call graph data available")
            return
        }
        
        graphPanel.loadGraph(graph)
        cardLayout.show(this, "graph")
        showingGraph = true
        updateButtonVisibility()
    }
    
    private fun showTree() {
        cardLayout.show(this, "tree")
        showingGraph = false
        updateButtonVisibility()
    }
    
    private fun updateButtonVisibility() {
        showGraphButton.isVisible = !showingGraph
        backButton.isVisible = showingGraph
    }
```

**Step 4: Modify performDetection to generate call graph**

In `performDetection()` method, after getting results, add call graph generation:

```kotlin
                    // After: val response = engine.detect(request)
                    results = response.results
                    summary = response.summary
                    
                    // Generate call graph
                    val tracer = SimpleCallGraphTracer()
                    currentGraph = tracer.traceCallGraph(projectPath, results)
                    
                    indicator.fraction = 1.0
```

**Step 5: Update updateResults to enable graph button**

Modify `updateResults` method:

```kotlin
    fun updateResults(results: List<DetectionResult>, summary: DetectionSummary) {
        SwingUtilities.invokeLater {
            currentResults = results
            currentSummary = summary
            
            // ... existing tree update code ...
            
            // Enable graph button if we have data
            showGraphButton.isEnabled = currentGraph?.nodes?.isNotEmpty() == true
            
            model.reload()
            expandAllNodes(tree, 0, tree.rowCount)
        }
    }
```

**Step 6: Verify compilation**

Run: `cd /Users/eric/development/idea-endpoint-plugin && ./gradlew :ui:compileKotlin --quiet`

Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add ui/src/main/kotlin/com/xxx/endpoint/ui/toolwindow/ResultTreePanel.kt
git commit -m "feat(ui): add graph toggle to ResultTreePanel"
```

---

## Task 7: Update SimpleCallGraphTracer for Graph Generation

**Files:**
- Modify: `core/src/main/kotlin/com/xxx/endpoint/core/tracer/SimpleCallGraphTracer.kt`

**Step 1: Add traceCallGraph method**

Check the existing SimpleCallGraphTracer and ensure it has a method that generates CallGraph from DetectionResult list. If not, add:

```kotlin
    fun traceCallGraph(projectPath: String, results: List<DetectionResult>): CallGraph {
        val nodes = mutableMapOf<String, CallGraphNode>()
        val edges = mutableListOf<CallEdge>()
        val entryPoints = mutableListOf<String>()
        
        results.forEach { result ->
            val entry = result.entry
            val nodeId = "${entry.containingFile}:${entry.name}"
            
            // Add traffic entry node
            nodes[nodeId] = CallGraphNode(
                id = nodeId,
                type = NodeType.TRAFFIC_ENTRY,
                name = entry.name,
                qualifiedName = entry.qualifiedName ?: entry.name,
                filePath = entry.containingFile,
                line = entry.line,
                metadata = mapOf(
                    "type" to entry.type.name,
                    "path" to (entry.path ?: "")
                )
            )
            entryPoints.add(nodeId)
            
            // Add call chain nodes if available
            result.callChain?.forEach { call ->
                val callNodeId = "${call.file}:${call.method}"
                if (nodes[callNodeId] == null) {
                    nodes[callNodeId] = CallGraphNode(
                        id = callNodeId,
                        type = NodeType.METHOD,
                        name = call.method,
                        qualifiedName = call.qualifiedName ?: call.method,
                        filePath = call.file,
                        line = call.line
                    )
                }
                edges.add(CallEdge(
                    from = nodeId,
                    to = callNodeId,
                    relation = CallRelation.CALLS
                ))
                nodeId = callNodeId // Chain the calls
            }
        }
        
        return CallGraph(
            nodes = nodes,
            edges = edges,
            entryPoints = entryPoints,
            changedNodes = emptySet(),
            metadata = GraphMetadata(
                project = projectPath,
                generatedAt = java.time.Instant.now().toString()
            )
        )
    }
```

**Step 2: Verify compilation**

Run: `cd /Users/eric/development/idea-endpoint-plugin && ./gradlew :core:compileKotlin --quiet`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add core/src/main/kotlin/com/xxx/endpoint/core/tracer/SimpleCallGraphTracer.kt
git commit -m "feat(core): add traceCallGraph method to SimpleCallGraphTracer"
```

---

## Task 8: Build and Test Plugin

**Files:**
- Build output: `plugin/build/distributions/`

**Step 1: Clean and build**

Run: `cd /Users/eric/development/idea-endpoint-plugin && ./gradlew clean buildPlugin`

Expected: BUILD SUCCESSFUL

**Step 2: Check plugin artifact**

Run: `ls -la /Users/eric/development/idea-endpoint-plugin/plugin/build/distributions/`

Expected: ZIP file exists (e.g., `idea-endpoint-plugin-*.zip`)

**Step 3: Manual test in IDEA**

1. Open IDEA 2025.3
2. Install plugin from disk (select the ZIP)
3. Open a project with Spring MVC endpoints
4. Open Endpoint Detector tool window
5. Click "检测变更"
6. Click "查看图谱"
7. Verify graph renders
8. Click a node → should navigate to source
9. Use zoom buttons (+/-/reset)
10. Search for a node name
11. Click "返回列表"

**Step 4: Commit if successful**

```bash
git add -A
git commit -m "build: graph visualization feature complete"
```

---

## Summary

| Task | Component | Files Created | Files Modified |
|------|-----------|---------------|----------------|
| 1 | GraphFormat enum | - | GraphExporter.kt |
| 2 | MermaidExporter | MermaidExporter.kt | - |
| 3 | HTML Template | graph-template.html | - |
| 4 | GraphJsBridge | GraphJsBridge.kt | - |
| 5 | GraphWebViewPanel | GraphWebViewPanel.kt | - |
| 6 | ResultTreePanel | - | ResultTreePanel.kt |
| 7 | SimpleCallGraphTracer | - | SimpleCallGraphTracer.kt |
| 8 | Build & Test | - | - |

---

## Verification Checklist

- [ ] MERMAID added to GraphFormat enum
- [ ] MermaidExporter generates valid Mermaid syntax
- [ ] HTML template has zoom, pan, search controls
- [ ] GraphJsBridge navigates to source on click
- [ ] GraphWebViewPanel loads and renders graphs
- [ ] ResultTreePanel toggles between tree and graph
- [ ] Plugin builds successfully
- [ ] Manual tests pass in IDEA 2025.3