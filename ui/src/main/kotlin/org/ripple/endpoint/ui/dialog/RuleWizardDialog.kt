package org.ripple.endpoint.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.ripple.endpoint.detector.api.model.CustomDetectorRule
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.NameExtractionRule
import org.ripple.endpoint.detector.api.model.NameExtractionType
import org.ripple.endpoint.detector.api.model.PathExtractionRule
import org.ripple.endpoint.detector.api.model.PathExtractionType
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class RuleWizardDialog(private val project: Project?) : DialogWrapper(project, true) {

    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)
    
    private val stepLabels = listOf("基本信息", "匹配规则", "提取规则", "确认")
    private var currentStep = 0
    
    private val nameField = JBTextField(30)
    private val descriptionField = JBTextField(30)
    
    private val filePatternsField = JBTextField(50)
    private val annotationsField = JBTextField(50)
    private val classNamesField = JBTextField(50)
    private val entryTypeCombo = JComboBox(EntryType.values())
    
    private val pathPatternField = JBTextField(30)
    private val pathDefaultField = JBTextField(20)
    
    private val summaryArea = JTextArea(10, 40)
    
    private val prevButton = JButton("上一步")
    private val nextButton = JButton("下一步")
    
    var resultRule: CustomDetectorRule? = null
        private set

    init {
        title = "引导式创建检测规则"
        
        createPanels()
        setupButtons()
        
        init()
        setOKActionEnabled(false)
    }

    private fun createPanels() {
        cardPanel.add(createStep1Panel(), "step0")
        cardPanel.add(createStep2Panel(), "step1")
        cardPanel.add(createStep3Panel(), "step2")
        cardPanel.add(createStep4Panel(), "step3")
    }
    
    private fun createStep1Panel(): JPanel {
        return JPanel(BorderLayout(10, 10)).apply {
            border = JBUI.Borders.empty(20)
            
            val formPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                
                add(JLabel("为你的 RPC 框架创建一个检测规则").apply { font = font.deriveFont(16f) })
                add(Box.createVerticalStrut(20))
                
                add(JLabel("规则名称 *"))
                add(nameField)
                add(JLabel("示例: MyRpc Service, Dubbo Provider"))
                add(Box.createVerticalStrut(15))
                
                add(JLabel("描述 (可选)"))
                add(descriptionField)
                add(JLabel("简要描述这个规则检测什么类型的入口"))
            }
            
            add(formPanel, BorderLayout.NORTH)
        }
    }
    
    private fun createStep2Panel(): JPanel {
        return JPanel(BorderLayout(10, 10)).apply {
            border = JBUI.Borders.empty(20)
            
            val formPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                
                add(JLabel("配置如何匹配你的 RPC 服务").apply { font = font.deriveFont(16f) })
                add(Box.createVerticalStrut(20))
                
                add(JLabel("文件路径模式"))
                add(filePatternsField)
                add(JLabel("使用 glob 模式，多个用逗号分隔。示例: **/rpc/**/*.java, **/provider/**/*.java"))
                add(Box.createVerticalStrut(15))
                
                add(JLabel("注解匹配"))
                add(annotationsField)
                add(JLabel("检测带有这些注解的类/方法。示例: @RpcService, @DubboService"))
                add(Box.createVerticalStrut(15))
                
                add(JLabel("类名模式"))
                add(classNamesField)
                add(JLabel("匹配类名模式。示例: *ServiceImpl, *Provider, *Handler"))
                add(Box.createVerticalStrut(15))
                
                add(JLabel("入口类型"))
                add(entryTypeCombo)
            }
            
            add(JScrollPane(formPanel), BorderLayout.CENTER)
        }
    }
    
    private fun createStep3Panel(): JPanel {
        return JPanel(BorderLayout(10, 10)).apply {
            border = JBUI.Borders.empty(20)
            
            val formPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                
                add(JLabel("配置如何提取服务路径和名称").apply { font = font.deriveFont(16f) })
                add(Box.createVerticalStrut(20))
                
                add(JLabel("路径提取正则"))
                add(pathPatternField)
                add(JLabel("从类名提取路径。示例: (.+)ServiceImpl 提取 ServiceImpl 前的部分"))
                add(Box.createVerticalStrut(15))
                
                add(JLabel("默认路径前缀"))
                add(pathDefaultField)
                add(JLabel("示例: /rpc, /api"))
            }
            
            add(formPanel, BorderLayout.NORTH)
        }
    }
    
    private fun createStep4Panel(): JPanel {
        return JPanel(BorderLayout(10, 10)).apply {
            border = JBUI.Borders.empty(20)
            
            val formPanel = JPanel(BorderLayout()).apply {
                add(JLabel("规则预览").apply { font = font.deriveFont(16f) }, BorderLayout.NORTH)
                
                summaryArea.isEditable = false
                add(JScrollPane(summaryArea), BorderLayout.CENTER)
            }
            
            add(formPanel, BorderLayout.CENTER)
        }
    }
    
    private fun setupButtons() {
        prevButton.addActionListener {
            if (currentStep > 0) {
                currentStep--
                cardLayout.show(cardPanel, "step$currentStep")
                updateButtons()
            }
        }
        
        nextButton.addActionListener {
            if (validateCurrentStep()) {
                if (currentStep < 3) {
                    currentStep++
                    if (currentStep == 3) {
                        updateSummary()
                    }
                    cardLayout.show(cardPanel, "step$currentStep")
                    updateButtons()
                }
            }
        }
    }
    
    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            0 -> {
                if (nameField.text.isBlank()) {
                    JOptionPane.showMessageDialog(contentPane, "请输入规则名称", "提示", JOptionPane.WARNING_MESSAGE)
                    false
                } else true
            }
            1 -> {
                val hasPattern = filePatternsField.text.isNotBlank() ||
                    annotationsField.text.isNotBlank() ||
                    classNamesField.text.isNotBlank()
                if (!hasPattern) {
                    JOptionPane.showMessageDialog(contentPane, "请至少配置一个匹配条件", "提示", JOptionPane.WARNING_MESSAGE)
                    false
                } else true
            }
            else -> true
        }
    }
    
    private fun updateSummary() {
        val sb = StringBuilder()
        sb.appendLine("规则名称: ${nameField.text}")
        sb.appendLine("描述: ${descriptionField.text}")
        sb.appendLine()
        sb.appendLine("匹配规则:")
        sb.appendLine("  文件模式: ${filePatternsField.text}")
        sb.appendLine("  注解: ${annotationsField.text}")
        sb.appendLine("  类名: ${classNamesField.text}")
        sb.appendLine("  入口类型: ${entryTypeCombo.selectedItem}")
        sb.appendLine()
        sb.appendLine("提取规则:")
        sb.appendLine("  路径正则: ${pathPatternField.text}")
        sb.appendLine("  默认路径: ${pathDefaultField.text}")
        
        summaryArea.text = sb.toString()
    }
    
    private fun updateButtons() {
        prevButton.isVisible = currentStep > 0
        nextButton.text = if (currentStep == 3) "完成" else "下一步"
        setOKActionEnabled(currentStep == 3)
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.preferredSize = Dimension(550, 420)
        
        panel.add(cardPanel, BorderLayout.CENTER)
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(prevButton)
            add(nextButton)
        }
        panel.add(buttonPanel, BorderLayout.SOUTH)
        
        return panel
    }
    
    override fun createSouthPanel(): JComponent? {
        return null
    }

    override fun doOKAction() {
        resultRule = CustomDetectorRule(
            id = "custom-${System.currentTimeMillis()}",
            name = nameField.text.trim(),
            description = descriptionField.text.trim(),
            enabled = true,
            filePatterns = parseList(filePatternsField.text),
            annotations = parseList(annotationsField.text),
            classNames = parseList(classNamesField.text),
            entryType = entryTypeCombo.selectedItem as EntryType,
            pathExtraction = if (pathPatternField.text.isNotBlank() || pathDefaultField.text.isNotBlank()) {
                PathExtractionRule(
                    type = PathExtractionType.FROM_CLASS_NAME,
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
        fun show(project: Project?): CustomDetectorRule? {
            val dialog = RuleWizardDialog(project)
            if (dialog.showAndGet()) {
                return dialog.resultRule
            }
            return null
        }
    }
}