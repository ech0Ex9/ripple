package org.ripple.endpoint.ui.settings

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import org.ripple.endpoint.detector.api.model.CustomDetectorRule
import org.ripple.endpoint.ui.config.DetectorRuleConfig
import org.ripple.endpoint.ui.dialog.RuleConfigDialog
import org.ripple.endpoint.ui.dialog.RuleWizardDialog
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class RuleManagementPanel(private val project: Project?) : JPanel(BorderLayout()) {
    
    private val ruleListModel = DefaultListModel<String>()
    private val ruleList = JBList(ruleListModel)
    private val rules = mutableListOf<CustomDetectorRule>()
    
    private val addButton = JButton("添加规则")
    private val wizardButton = JButton("向导创建")
    private val editButton = JButton("编辑")
    private val deleteButton = JButton("删除")
    private val resetButton = JButton("重置默认")
    
    init {
        border = JBUI.Borders.empty(10)
        
        setupList()
        setupButtons()
        loadRules()
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(addButton)
            add(wizardButton)
            add(editButton)
            add(deleteButton)
            add(resetButton)
        }
        
        add(JScrollPane(ruleList), BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }
    
    private fun setupList() {
        ruleList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        ruleList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                updateButtonStates()
            }
        }
    }
    
    private fun setupButtons() {
        addButton.addActionListener {
            val rule = RuleConfigDialog.show(project)
            if (rule != null) {
                DetectorRuleConfig.getInstance().addRule(rule)
                loadRules()
            }
        }
        
        wizardButton.addActionListener {
            val rule = RuleWizardDialog.show(project)
            if (rule != null) {
                DetectorRuleConfig.getInstance().addRule(rule)
                loadRules()
            }
        }
        
        editButton.addActionListener {
            val index = ruleList.selectedIndex
            if (index >= 0 && index < rules.size) {
                val existing = rules[index]
                val updated = RuleConfigDialog.show(project, existing)
                if (updated != null) {
                    DetectorRuleConfig.getInstance().updateRule(updated)
                    loadRules()
                }
            }
        }
        
        deleteButton.addActionListener {
            val index = ruleList.selectedIndex
            if (index >= 0 && index < rules.size) {
                val rule = rules[index]
                val confirm = JOptionPane.showConfirmDialog(
                    this,
                    "确定删除规则 \"${rule.name}\"?",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION
                )
                if (confirm == JOptionPane.YES_OPTION) {
                    DetectorRuleConfig.getInstance().removeRule(rule.id)
                    loadRules()
                }
            }
        }
        
        resetButton.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(
                this,
                "确定重置为默认规则? 自定义规则将被清除。",
                "确认重置",
                JOptionPane.YES_NO_OPTION
            )
            if (confirm == JOptionPane.YES_OPTION) {
                DetectorRuleConfig.getInstance().resetToDefaults()
                loadRules()
            }
        }
    }
    
    private fun loadRules() {
        rules.clear()
        rules.addAll(DetectorRuleConfig.getInstance().rules)
        
        ruleListModel.clear()
        rules.forEach { rule ->
            val status = if (rule.enabled) "✓" else "✗"
            ruleListModel.addElement("$status ${rule.name} - ${rule.description}")
        }
        
        updateButtonStates()
    }
    
    private fun updateButtonStates() {
        val hasSelection = ruleList.selectedIndex >= 0
        editButton.isEnabled = hasSelection
        deleteButton.isEnabled = hasSelection
    }
    
fun applyChanges() = Unit
}