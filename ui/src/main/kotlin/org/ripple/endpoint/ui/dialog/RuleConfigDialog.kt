package org.ripple.endpoint.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.ripple.endpoint.detector.api.model.CustomDetectorRule
import org.ripple.endpoint.detector.api.model.EntryType
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class RuleConfigDialog(
    private val project: Project?,
    private val existingRule: CustomDetectorRule? = null
) : DialogWrapper(project, true) {

    private val nameField = JBTextField(30)
    private val descriptionField = JBTextField(30)
    private val enabledCheckbox = JCheckBox("启用", true)
    private val filePatternsField = JBTextField(50)
    private val annotationsField = JBTextField(50)
    private val classNamesField = JBTextField(50)
    private val entryTypeCombo = JComboBox(EntryType.values())
    
    private val pathPatternField = JBTextField(30)
    private val pathDefaultField = JBTextField(20)
    
    var resultRule: CustomDetectorRule? = null
        private set

    init {
        title = if (existingRule != null) "编辑检测规则" else "新建检测规则"
        
        existingRule?.let { rule ->
            nameField.text = rule.name
            descriptionField.text = rule.description
            enabledCheckbox.isSelected = rule.enabled
            filePatternsField.text = rule.filePatterns.joinToString(", ")
            annotationsField.text = rule.annotations.joinToString(", ")
            classNamesField.text = rule.classNames.joinToString(", ")
            entryTypeCombo.selectedItem = rule.entryType
            rule.pathExtraction?.let { pe ->
                pathPatternField.text = pe.pattern
                pathDefaultField.text = pe.defaultValue
            }
        }
        
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.border = JBUI.Borders.empty(10)
        panel.preferredSize = Dimension(550, 400)
        
        val formPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            add(createFieldRow("规则名称:", nameField))
            add(Box.createVerticalStrut(8))
            add(createFieldRow("描述:", descriptionField))
            add(Box.createVerticalStrut(8))
            add(createFieldRow("启用:", enabledCheckbox))
            add(Box.createVerticalStrut(12))
            
            add(JSeparator())
            add(Box.createVerticalStrut(12))
            
            add(createFieldRow("文件模式:", filePatternsField, "如: **/rpc/**/*.java, 多个用逗号分隔"))
            add(Box.createVerticalStrut(8))
            add(createFieldRow("注解匹配:", annotationsField, "如: @RpcService, 多个用逗号分隔"))
            add(Box.createVerticalStrut(8))
            add(createFieldRow("类名模式:", classNamesField, "如: *ServiceImpl, *Provider"))
            add(Box.createVerticalStrut(8))
            add(createFieldRow("入口类型:", entryTypeCombo))
            add(Box.createVerticalStrut(12))
            
            add(JSeparator())
            add(Box.createVerticalStrut(12))
            
            add(createFieldRow("路径提取正则:", pathPatternField, "如: (.+)ServiceImpl"))
            add(Box.createVerticalStrut(8))
            add(createFieldRow("默认路径:", pathDefaultField, "如: /rpc"))
        }
        
        panel.add(JScrollPane(formPanel), BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createFieldRow(label: String, component: JComponent, hint: String = ""): JPanel {
        return JPanel(BorderLayout(5, 0)).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel(label).apply { preferredSize = Dimension(100, 24) })
            }, BorderLayout.WEST)
            
            val centerPanel = JPanel(BorderLayout())
            centerPanel.add(component, BorderLayout.NORTH)
            if (hint.isNotEmpty()) {
                centerPanel.add(JLabel(hint).apply { 
                    foreground = java.awt.Color.GRAY 
                    font = font.deriveFont(10f)
                }, BorderLayout.SOUTH)
            }
            add(centerPanel, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(contentPane, "请输入规则名称", "提示", JOptionPane.WARNING_MESSAGE)
            return
        }
        
        val filePatterns = parseList(filePatternsField.text)
        val annotations = parseList(annotationsField.text)
        val classNames = parseList(classNamesField.text)
        
        if (filePatterns.isEmpty() && annotations.isEmpty() && classNames.isEmpty()) {
            JOptionPane.showMessageDialog(contentPane, "请至少配置一个匹配条件", "提示", JOptionPane.WARNING_MESSAGE)
            return
        }
        
        val id = existingRule?.id ?: "custom-${System.currentTimeMillis()}"
        
        resultRule = CustomDetectorRule(
            id = id,
            name = name,
            description = descriptionField.text.trim(),
            enabled = enabledCheckbox.isSelected,
            filePatterns = filePatterns,
            annotations = annotations,
            classNames = classNames,
            entryType = entryTypeCombo.selectedItem as EntryType,
            pathExtraction = if (pathPatternField.text.isNotBlank() || pathDefaultField.text.isNotBlank()) {
                org.ripple.endpoint.detector.api.model.PathExtractionRule(
                    type = org.ripple.endpoint.detector.api.model.PathExtractionType.FROM_CLASS_NAME,
                    pattern = pathPatternField.text.trim(),
                    defaultValue = pathDefaultField.text.trim()
                )
            } else null
        )
        
        super.doOKAction()
    }
    
    private fun parseList(text: String): List<String> {
        return text.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    companion object {
        fun show(project: Project?, existingRule: CustomDetectorRule? = null): CustomDetectorRule? {
            val dialog = RuleConfigDialog(project, existingRule)
            if (dialog.showAndGet()) {
                return dialog.resultRule
            }
            return null
        }
    }
}