# Graph Visualization Design

> Date: 2026-03-13
> Status: Approved
> Author: Sisyphus (AI Agent)

---

## Overview

Add interactive call graph visualization to the IDEA plugin using JCEF WebView + Mermaid.js. Users can toggle between tree view and graph view, with full interactivity including zoom, pan, highlight, search, and click-to-navigate.

---

## Requirements

### Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Add "查看图谱" button to toolbar | Must |
| FR-2 | Toggle between tree view and graph view | Must |
| FR-3 | Render call graph as Mermaid flowchart | Must |
| FR-4 | Click node to navigate to source file:line | Must |
| FR-5 | Zoom and pan the graph | Must |
| FR-6 | Highlight node and its direct connections | Must |
| FR-7 | Search/filter nodes by name | Must |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-1 | Graph renders within 2 seconds for <200 nodes |
| NFR-2 | Graceful degradation when JCEF unavailable |
| NFR-3 | Support IDEA dark theme |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ResultTreePanel                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Toolbar: [检测变更] [生成报告] [查看图谱] [返回列表] │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌───────────────────┐    ┌───────────────────────────┐   │
│  │   TreePanel       │    │   GraphWebViewPanel       │   │
│  │   (existing)      │◄──►│   (new)                   │   │
│  └───────────────────┘    │  ┌─────────────────────┐  │   │
│                           │  │ JCEF Browser        │  │   │
│                           │  │ ┌─────────────────┐ │  │   │
│                           │  │ │ Mermaid.js      │ │  │   │
│                           │  │ │ Flowchart       │ │  │   │
│                           │  │ └─────────────────┘ │  │   │
│                           │  └─────────────────────┘  │   │
│                           └───────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Details

### 1. MermaidExporter

**Location**: `core/src/main/kotlin/com/xxx/endpoint/core/graph/MermaidExporter.kt`

**Responsibility**: Convert `CallGraph` to Mermaid flowchart syntax

**API**:
```kotlin
class MermaidExporter : GraphExporter {
    override val format: GraphFormat = GraphFormat.MERMAID
    override val fileExtension: String = "mmd"
    
    override fun export(graph: CallGraph): String
}
```

**Node Styling Rules**:

| NodeType | Mermaid Shape | CSS Class | Color |
|----------|---------------|-----------|-------|
| TRAFFIC_ENTRY | `([name])` | `traffic-entry` | Red (#ff6b6b) |
| CLASS | `[name]` | `class-node` | Blue (#4dabf7) |
| METHOD | `[name]` | `method-node` | Yellow (#ffd43b) |
| FIELD | `[name]` | `field-node` | Green (#69db7c) |
| EXTERNAL | `[name]` | `external-node` | Gray (#adb5bd) |

**Changed Nodes**: Add `:::changed` class with thick border

**Edge Styling**:

| CallRelation | Line Style |
|--------------|------------|
| CALLS | solid |
| IMPLEMENTS | dashed |
| REFERENCES | dotted |
| ANNOTATED_BY | bold |

### 2. GraphWebViewPanel

**Location**: `ui/src/main/kotlin/com/xxx/endpoint/ui/graph/GraphWebViewPanel.kt`

**Responsibility**: JCEF browser container with graph rendering

**API**:
```kotlin
class GraphWebViewPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    /**
     * Load and render a call graph
     */
    fun loadGraph(graph: CallGraph)
    
    /**
     * Highlight a specific node and its direct connections
     */
    fun highlightNode(nodeId: String)
    
    /**
     * Filter nodes by search query
     */
    fun filterNodes(query: String)
    
    /**
     * Reset zoom and pan to default
     */
    fun resetView()
}
```

**Dependencies**:
- `com.intellij.ui.jcef.JBCefBrowser`
- `com.intellij.ui.jcef.JBCefJSQuery`

### 3. GraphJsBridge

**Location**: `ui/src/main/kotlin/com/xxx/endpoint/ui/graph/GraphJsBridge.kt`

**Responsibility**: Java ↔ JavaScript communication bridge

**API**:
```kotlin
class GraphJsBridge(
    private val project: Project,
    private val nodeRegistry: Map<String, CallGraphNode>
) {
    
    /**
     * Called from JavaScript when user clicks a node
     * Navigates to the source file at the node's line
     */
    @JavascriptInterface
    fun onNodeClick(nodeId: String)
    
    /**
     * Get current graph data for search/filter
     */
    @JavascriptInterface
    fun getNodes(): String  // JSON array of node IDs
}
```

**Navigation Implementation**:
```kotlin
fun onNodeClick(nodeId: String) {
    val node = nodeRegistry[nodeId] ?: return
    val file = VirtualFileManager.getInstance()
        .findFileByUrl("file://${node.filePath}") ?: return
    
    val descriptor = OpenFileDescriptor(project, file, node.line ?: 0)
    FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
}
```

### 4. HTML Template

**Location**: `ui/src/main/resources/graph-template.html`

**Features**:
- Mermaid.js loaded from CDN
- Dark theme support via `prefers-color-scheme`
- Toolbar with search and zoom controls
- SVG pan/zoom using CSS transforms
- Node click → `window.jsBridge.onNodeClick(id)`

**Key JavaScript Functions**:
```javascript
// Initialize Mermaid
mermaid.initialize({
    startOnLoad: true,
    theme: document.body.classList.contains('dark') ? 'dark' : 'default',
    flowchart: { curve: 'basis' }
});

// Zoom controls
let scale = 1;
function zoomIn() { scale = Math.min(scale + 0.2, 3); applyTransform(); }
function zoomOut() { scale = Math.max(scale - 0.2, 0.3); applyTransform(); }

// Search filter
function filterNodes(query) {
    // Hide nodes not matching query
}

// Node click handler
document.addEventListener('click', (e) => {
    const nodeId = e.target.closest('.node')?.id;
    if (nodeId && window.jsBridge) {
        window.jsBridge.onNodeClick(nodeId);
    }
});
```

---

## Data Flow

```
┌────────────┐     ┌─────────────────┐     ┌───────────────┐
│ CallGraph  │────►│ MermaidExporter │────►│ Mermaid Syntax│
└────────────┘     └─────────────────┘     └───────┬───────┘
                                                   │
                                                   ▼
┌────────────────────────────────────────────────────────────┐
│                     HTML Template                          │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐  │
│  │ Search Box  │  │ Zoom Controls│  │ Mermaid Diagram  │  │
│  └─────────────┘  └─────────────┘  └────────┬─────────┘  │
└─────────────────────────────────────────────┼────────────┘
                                              │
                    ┌─────────────────────────┼─────────────────┐
                    │                         ▼                 │
                    │  User clicks node ──► JSBridge.onNodeClick│
                    │                         │                 │
                    │                         ▼                 │
                    │              OpenFileDescriptor           │
                    │                         │                 │
                    │                         ▼                 │
                    │              Navigate to source           │
                    └───────────────────────────────────────────┘
```

---

## UI Changes

### ResultTreePanel Modifications

**Current Toolbar**:
```
[检测变更] [生成报告]
```

**New Toolbar**:
```
[检测变更] [生成报告] [查看图谱] [返回列表]
                                ▲         ▲
                                │         │
                                │    Only visible when showing graph
                                │
                          Only visible when showing tree (after detection)
```

**Panel Swap Logic**:
```kotlin
private var showingGraph = false
private val cardLayout = CardLayout()

fun showGraph(graph: CallGraph) {
    graphPanel.loadGraph(graph)
    cardLayout.show(this, "graph")
    showingGraph = true
    updateToolbar()
}

fun showTree() {
    cardLayout.show(this, "tree")
    showingGraph = false
    updateToolbar()
}
```

---

## GraphFormat Enum Extension

**Current**:
```kotlin
enum class GraphFormat {
    DOT,
    GRAPHML,
    JSON
}
```

**Extended**:
```kotlin
enum class GraphFormat {
    DOT,
    GRAPHML,
    JSON,
    MERMAID  // New
}
```

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| JCEF not available | Show dialog: "Graph visualization requires JCEF support. Please use a JetBrains Runtime." |
| Empty graph | Show message in panel: "No call graph data available. Run detection first." |
| Large graph (>500 nodes) | Show warning dialog with option to limit display depth to 3 levels |
| Mermaid.js CDN unreachable | Fall back to bundled mermaid.min.js |

---

## Testing Strategy

### Unit Tests

1. `MermaidExporterTest`
   - Export empty graph
   - Export graph with single node
   - Export graph with all node types
   - Verify styling for changed nodes
   - Verify edge styles

### Integration Tests

1. `GraphWebViewPanelTest`
   - Load graph renders without error
   - Search filters correctly
   - Zoom in/out works

### Manual Tests

1. Run plugin in IDEA 2025.3
2. Detect changes in demo project
3. Click "查看图谱" button
4. Verify graph renders
5. Click node → navigate to source
6. Use zoom/pan controls
7. Search for node name
8. Toggle back to tree view

---

## File Structure

```
idea-endpoint-plugin/
├── core/src/main/kotlin/com/xxx/endpoint/core/
│   └── graph/
│       ├── GraphExporter.kt          # Modified: add MERMAID to enum
│       └── MermaidExporter.kt        # New
│
├── ui/src/main/kotlin/com/xxx/endpoint/ui/
│   ├── graph/
│   │   ├── GraphWebViewPanel.kt      # New
│   │   └── GraphJsBridge.kt          # New
│   │
│   └── toolwindow/
│       └── ResultTreePanel.kt        # Modified: add graph panel
│
└── ui/src/main/resources/
    └── graph-template.html           # New
```

---

## Dependencies

No new external dependencies required. JCEF is bundled with JetBrains Runtime.

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| JCEF unavailable in some environments | Medium | High | Graceful error message + fallback |
| Large graphs cause performance issues | Medium | Medium | Limit depth + lazy rendering |
| Mermaid CDN unreachable | Low | Low | Bundle mermaid.min.js locally |

---

## Future Enhancements

1. **Export to SVG/PNG** - Add export button to save graph as image
2. **Layout options** - Allow users to switch between TB, LR, RL layouts
3. **Node collapse** - Collapse/expand subgraphs
4. **Diff view** - Show before/after call graph comparison