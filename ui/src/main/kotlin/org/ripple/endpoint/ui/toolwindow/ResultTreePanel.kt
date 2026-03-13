package org.ripple.endpoint.ui.toolwindow

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import org.ripple.endpoint.core.engine.DefaultDetectionEngine
import org.ripple.endpoint.core.engine.DetectionRequest
import org.ripple.endpoint.core.git.DefaultGitChangeResolver
import org.ripple.endpoint.core.model.DetectionResult
import org.ripple.endpoint.core.model.DetectionSummary
import org.ripple.endpoint.core.model.TypeStats
import org.ripple.endpoint.detector.api.model.ChangedFile
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.ImpactLevel
import org.ripple.endpoint.detector.grpc.GrpcDetector
import org.ripple.endpoint.detector.mq.MqDetector
import org.ripple.endpoint.detector.scheduled.ScheduledDetector
import org.ripple.endpoint.detector.spring.SpringMvcDetector
import org.ripple.endpoint.ui.dialog.BranchDiffDialog
import org.ripple.endpoint.ui.notification.EndpointNotifier
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class ResultTreePanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val tree: Tree
    private val rootNode = DefaultMutableTreeNode("点击「检测变更」或「分支比对」开始")
    private val model = DefaultTreeModel(rootNode)
    private val summaryLabel = JBLabel(" ")
    
    private var currentResults: List<DetectionResult> = emptyList()
    private var currentSummary: DetectionSummary? = null
    private var currentDiffInfo: String? = null
    
    init {
        tree = Tree(model).apply {
            isRootVisible = true
            cellRenderer = ResultTreeCellRenderer()
        }
        
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("检测变更").apply {
                toolTipText = "检测当前工作区的变更"
                addActionListener { performWorkingChangesDetection() }
            })
            add(JButton("分支比对").apply {
                toolTipText = "比对两个分支的差异并检测受影响的流量入口"
                addActionListener { performBranchDiffDetection() }
            })
            add(JButton("生成报告").apply {
                addActionListener { generateReport() }
            })
        }
        
        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)
        add(summaryLabel.apply { border = JBUI.Borders.empty(5) }, BorderLayout.SOUTH)
        border = JBUI.Borders.empty(10)
    }
    
    private fun performWorkingChangesDetection() {
        val projectPath = project.basePath ?: return
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "检测流量入口", true) {
            private var results: List<DetectionResult> = emptyList()
            private var summary: DetectionSummary? = null
            private var errorMessage: String? = null
            
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在解析 Git 变更..."
                indicator.fraction = 0.2
                
                try {
                    val gitResolver = DefaultGitChangeResolver()
                    val workingChanges = gitResolver.resolveWorkingChanges(projectPath)
                    
                    if (workingChanges.isEmpty()) {
                        errorMessage = "无 Git 变更文件"
                        return
                    }
                    
                    indicator.text = "正在检测流量入口..."
                    indicator.fraction = 0.5
                    
                    results = detectEntries(projectPath, workingChanges)
                    summary = buildSummary(results)
                    
                    indicator.fraction = 1.0
                    
                } catch (e: Exception) {
                    errorMessage = e.message
                }
            }
            
            override fun onSuccess() {
                if (errorMessage != null) {
                    showMessage(errorMessage!!)
                    EndpointNotifier.notifyNoChanges(project)
                    return
                }
                
                currentDiffInfo = null
                updateResults(results, summary ?: return)
                EndpointNotifier.notifyDetectionComplete(project, summary!!)
            }
            
            override fun onThrowable(error: Throwable) {
                showMessage("检测失败: ${error.message}")
                EndpointNotifier.notifyError(project, error.message ?: "Unknown error")
            }
        })
    }
    
    private fun performBranchDiffDetection() {
        val projectPath = project.basePath ?: return
        
        val gitResolver = DefaultGitChangeResolver()
        val branches = gitResolver.getBranches(projectPath)
        val currentBranch = gitResolver.getCurrentBranch(projectPath)
        
        if (branches.isEmpty()) {
            EndpointNotifier.notifyError(project, "无法获取分支列表")
            return
        }
        
        val selected = BranchDiffDialog.show(project, branches, currentBranch)
        if (selected == null) return
        
        val (sourceBranch, targetBranch) = selected
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "分支比对检测", true) {
            private var results: List<DetectionResult> = emptyList()
            private var summary: DetectionSummary? = null
            private var errorMessage: String? = null
            private var diffInfo: String? = null
            
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在比对分支..."
                indicator.fraction = 0.1
                
                try {
                    val diffResult = gitResolver.resolveBranchDiff(projectPath, sourceBranch, targetBranch)
                    
                    if (diffResult.changes.isEmpty()) {
                        errorMessage = "两个分支之间没有差异"
                        return
                    }
                    
                    diffInfo = "${sourceBranch} vs ${targetBranch} (${diffResult.ahead} commits ahead, ${diffResult.behind} behind)"
                    
                    indicator.text = "正在检测流量入口 (${diffResult.changes.size} 个变更文件)..."
                    indicator.fraction = 0.3
                    
                    results = detectEntries(projectPath, diffResult.changes)
                    summary = buildSummary(results)
                    
                    indicator.fraction = 1.0
                    
                } catch (e: Exception) {
                    errorMessage = e.message
                }
            }
            
            override fun onSuccess() {
                if (errorMessage != null) {
                    showMessage(errorMessage!!)
                    EndpointNotifier.notifyInfo(project, errorMessage!!)
                    return
                }
                
                currentDiffInfo = diffInfo
                updateResults(results, summary ?: return, diffInfo)
                EndpointNotifier.notifyDetectionComplete(project, summary!!)
            }
            
            override fun onThrowable(error: Throwable) {
                showMessage("检测失败: ${error.message}")
                EndpointNotifier.notifyError(project, error.message ?: "Unknown error")
            }
        })
    }
    
    private fun detectEntries(projectPath: String, changedFiles: List<ChangedFile>): List<DetectionResult> {
        val detectors = listOf(
            SpringMvcDetector(),
            GrpcDetector(),
            MqDetector(),
            ScheduledDetector()
        )
        
        val engine = DefaultDetectionEngine(detectors)
        val config = detectors.associate { it.name to it.defaultConfig }
        
        val request = DetectionRequest(
            projectPath = projectPath,
            changedFiles = changedFiles,
            config = config
        )
        
        return engine.detect(request).results
    }
    
    private fun buildSummary(results: List<DetectionResult>): DetectionSummary {
        val byType = results.groupBy { it.entry.type.name }
            .mapValues { (_, list) ->
                TypeStats(
                    total = list.size,
                    affected = list.size,
                    highRisk = list.count { it.impactLevel == ImpactLevel.HIGH }
                )
            }
        
        val byImpact = results.groupBy { it.impactLevel }
            .mapValues { it.value.size }
        
        return DetectionSummary(
            totalEntries = results.size,
            affectedEntries = results.size,
            byType = byType,
            byImpact = byImpact
        )
    }
    
    private fun generateReport() {
        if (currentResults.isEmpty()) {
            EndpointNotifier.notifyError(project, "没有检测结果，请先执行检测")
            return
        }
        
        try {
            val reportDir = Path.of(project.basePath ?: return, "flow-detector-report")
            Files.createDirectories(reportDir)
            
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val reportPath = reportDir.resolve("detection-report-$timestamp.md")
            
            val content = buildMarkdownReport()
            Files.writeString(reportPath, content)
            
            showMessage("报告已生成: ${reportPath.fileName}")
            EndpointNotifier.notifyInfo(project, "报告已保存到: $reportPath")
        } catch (e: Exception) {
            EndpointNotifier.notifyError(project, "生成报告失败: ${e.message}")
        }
    }
    
    private fun buildMarkdownReport(): String {
        return buildString {
            appendLine("# 流量入口检测报告")
            appendLine()
            
            currentDiffInfo?.let {
                appendLine("**比对信息**: $it")
                appendLine()
            }
            
            appendLine("生成时间: ${LocalDateTime.now()}")
            appendLine()
            
            val summary = currentSummary
            if (summary != null) {
                appendLine("## 统计")
                appendLine("- 总入口数: ${summary.totalEntries}")
                appendLine("- 受影响入口: ${summary.affectedEntries}")
                appendLine()
            }
            
            appendLine("## 受影响的流量入口")
            appendLine()
            
            val grouped = currentResults.groupBy { it.impactLevel }
            
            ImpactLevel.values().reversed().forEach { level ->
                val results = grouped[level] ?: return@forEach
                appendLine("### ${levelIcon(level)} (${results.size})")
                appendLine()
                results.forEach { r ->
                    val description = r.entry.description?.let { " - $it" } ?: ""
                    appendLine("- **${typeIcon(r.entry.type)}**: ${r.entry.name}${description}")
                    appendLine("  - 路径: ${r.entry.path ?: "N/A"}")
                    appendLine("  - 文件: ${r.entry.containingFile}:${r.entry.line}")
                    appendLine("  - 原因: ${r.impactReason}")
                    appendLine()
                }
            }
        }
    }
    
    fun updateResults(results: List<DetectionResult>, summary: DetectionSummary, diffInfo: String? = null) {
        SwingUtilities.invokeLater {
            currentResults = results
            currentSummary = summary
            
            rootNode.removeAllChildren()
            
            val rootText = if (diffInfo != null) {
                "分支比对: $diffInfo"
            } else {
                "工作区变更检测"
            }
            rootNode.userObject = rootText
            
            if (results.isEmpty()) {
                rootNode.add(DefaultMutableTreeNode("无受影响的流量入口"))
            } else {
                val groupedByImpact = results.groupBy { it.impactLevel }
                
                ImpactLevel.values().reversed().forEach { level ->
                    val levelResults = groupedByImpact[level] ?: return@forEach
                    val levelNode = DefaultMutableTreeNode("${levelIcon(level)} (${levelResults.size})")
                    
                    levelResults.forEach { result ->
                        val entryNode = DefaultMutableTreeNode(result)
                        entryNode.add(DefaultMutableTreeNode("📄 文件: ${result.entry.containingFile}:${result.entry.line}"))
                        entryNode.add(DefaultMutableTreeNode("📝 原因: ${result.impactReason}"))
                        levelNode.add(entryNode)
                    }
                    
                    rootNode.add(levelNode)
                }
            }
            
            summaryLabel.text = "共 ${summary.totalEntries} 个入口，受影响 ${summary.affectedEntries} 个"
            
            model.reload()
            expandAllNodes(tree, 0, tree.rowCount)
        }
    }
    
    fun showMessage(message: String) {
        SwingUtilities.invokeLater {
            rootNode.removeAllChildren()
            rootNode.add(DefaultMutableTreeNode(message))
            model.reload()
        }
    }
    
    private fun expandAllNodes(tree: JTree, startingIndex: Int, rowCount: Int) {
        for (i in startingIndex until rowCount) {
            tree.expandRow(i)
        }
        if (tree.rowCount > rowCount) {
            expandAllNodes(tree, rowCount, tree.rowCount)
        }
    }
    
    private fun levelIcon(level: ImpactLevel): String {
        return when (level) {
            ImpactLevel.HIGH -> "🔴 高风险"
            ImpactLevel.MEDIUM -> "🟡 中风险"
            ImpactLevel.LOW -> "🟢 低风险"
        }
    }
    
    private fun typeIcon(type: EntryType): String {
        return when (type) {
            EntryType.HTTP -> "🌐 HTTP"
            EntryType.GRPC -> "⚡ gRPC"
            EntryType.MQ -> "📨 MQ"
            EntryType.SCHEDULED -> "⏰ 定时任务"
            EntryType.CUSTOM -> "🔧 自定义"
        }
    }
    
    inner class ResultTreeCellRenderer : DefaultTreeCellRenderer() {
        init {
            backgroundNonSelectionColor = null
            backgroundSelectionColor = null
        }
        
        override fun getTreeCellRendererComponent(
            tree: JTree?,
            value: Any?,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
            
            isOpaque = false
            
            when (val userObject = (value as? DefaultMutableTreeNode)?.userObject) {
                is DetectionResult -> {
                    val result = userObject
                    val description = result.entry.description?.let { " - $it" } ?: ""
                    text = "${typeIcon(result.entry.type)} ${result.entry.name}${description}: ${result.entry.path ?: ""}"
                }
            }
            
            return this
        }
    }
}