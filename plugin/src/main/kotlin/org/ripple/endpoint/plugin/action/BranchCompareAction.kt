package org.ripple.endpoint.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBComboBoxLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import org.ripple.endpoint.core.git.DefaultGitChangeResolver
import org.ripple.endpoint.ui.notification.EndpointNotifier
import java.awt.BorderLayout
import javax.swing.JComboBox
import javax.swing.JPanel

class BranchCompareAction : AnAction("分支对比检测", "对比两个分支的变更影响", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val dialog = BranchCompareDialog(project)
        if (dialog.showAndGet()) {
            performBranchCompare(project, dialog.sourceBranch, dialog.targetBranch)
        }
    }
    
    private fun performBranchCompare(project: Project, sourceBranch: String, targetBranch: String) {
        EndpointNotifier.notifyDetectionStarted(project)
        
        try {
            val gitResolver = DefaultGitChangeResolver()
            val diffResult = gitResolver.resolveBranchDiff(
                project.basePath ?: return,
                sourceBranch,
                targetBranch
            )
            
            // TODO: Perform detection and generate report
            
            EndpointNotifier.notifyDetectionComplete(
                project,
                org.ripple.endpoint.core.model.DetectionSummary(
                    totalEntries = 0,
                    affectedEntries = diffResult.changes.size,
                    byType = emptyMap(),
                    byImpact = emptyMap()
                )
            )
            
        } catch (e: Exception) {
            EndpointNotifier.notifyError(project, e.message ?: "Unknown error")
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

class BranchCompareDialog(private val project: Project) : DialogWrapper(project) {
    
    private val gitResolver = DefaultGitChangeResolver()
    private val branches = gitResolver.getBranches(project.basePath ?: "")
    
    private val sourceBranchCombo = JComboBox(branches.toTypedArray())
    private val targetBranchCombo = JComboBox(branches.toTypedArray())
    
    val sourceBranch: String
        get() = sourceBranchCombo.selectedItem as? String ?: ""
    
    val targetBranch: String
        get() = targetBranchCombo.selectedItem as? String ?: ""
    
    init {
        title = "分支对比检测"
        init()
        
        // Set defaults
        val currentBranch = gitResolver.getCurrentBranch(project.basePath ?: "")
        if (currentBranch != null) {
            sourceBranchCombo.selectedItem = currentBranch
        }
        if (branches.contains("master")) {
            targetBranchCombo.selectedItem = "master"
        } else if (branches.contains("main")) {
            targetBranchCombo.selectedItem = "main"
        }
    }
    
    override fun createCenterPanel(): JPanel {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("源分支:", sourceBranchCombo)
            .addLabeledComponent("目标分支:", targetBranchCombo)
            .panel
    }
}