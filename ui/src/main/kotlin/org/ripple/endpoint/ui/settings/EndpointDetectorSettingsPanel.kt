package org.ripple.endpoint.ui.settings

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

class EndpointDetectorSettingsPanel(private val project: Project) {
    
    private val commitCheckEnabled = JBCheckBox("启用 Commit 前检测")
    private val highRiskBlockCommit = JBCheckBox("高风险变更拦截 Commit")
    private val reportPathField = JBTextField("flow-detector-report", 30)
    private val maxTraceDepthField = JBTextField("20", 5)
    
    private var originalCommitCheckEnabled = true
    private var originalHighRiskBlockCommit = true
    private var originalReportPath = "flow-detector-report"
    private var originalMaxTraceDepth = "20"
    
    fun createPanel(): JPanel {
        return FormBuilder.createFormBuilder()
            .addComponent(createTitleLabel("通用配置"))
            .addComponent(commitCheckEnabled)
            .addComponent(highRiskBlockCommit)
            .addLabeledComponent("报告保存路径:", createPathPanel())
            .addLabeledComponent("最大追踪深度:", maxTraceDepthField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
            .also { 
                it.border = JBUI.Borders.empty(20)
                loadSettings()
            }
    }
    
    private fun createTitleLabel(text: String): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JBLabel(text).apply { font = font.deriveFont(java.awt.Font.BOLD) })
        }
    }
    
    private fun createPathPanel(): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(reportPathField)
            add(JButton("浏览...").apply {
                addActionListener {
                    // TODO: File chooser dialog
                }
            })
        }
    }
    
    private fun loadSettings() {
        // Load from config repository
        originalCommitCheckEnabled = true
        originalHighRiskBlockCommit = true
        originalReportPath = "flow-detector-report"
        originalMaxTraceDepth = "20"
        
        commitCheckEnabled.isSelected = originalCommitCheckEnabled
        highRiskBlockCommit.isSelected = originalHighRiskBlockCommit
        reportPathField.text = originalReportPath
        maxTraceDepthField.text = originalMaxTraceDepth
    }
    
    fun isModified(): Boolean {
        return commitCheckEnabled.isSelected != originalCommitCheckEnabled
            || highRiskBlockCommit.isSelected != originalHighRiskBlockCommit
            || reportPathField.text != originalReportPath
            || maxTraceDepthField.text != originalMaxTraceDepth
    }
    
    fun apply() {
        // Save to config repository
    }
    
    fun reset() {
        commitCheckEnabled.isSelected = originalCommitCheckEnabled
        highRiskBlockCommit.isSelected = originalHighRiskBlockCommit
        reportPathField.text = originalReportPath
        maxTraceDepthField.text = originalMaxTraceDepth
    }
}