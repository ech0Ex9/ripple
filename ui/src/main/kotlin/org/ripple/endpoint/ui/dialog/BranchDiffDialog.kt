package org.ripple.endpoint.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class BranchDiffDialog(
    private val project: Project,
    private val branches: List<String>,
    private val currentBranch: String?
) : DialogWrapper(project) {

    private val sourceBranchCombo = JComboBox(branches.toTypedArray())
    private val targetBranchCombo = JComboBox(branches.toTypedArray())
    
    var sourceBranch: String? = null
        private set
    var targetBranch: String? = null
        private set

    init {
        title = "分支比对检测"
        setOKButtonText("开始检测")
        
        if (currentBranch != null) {
            sourceBranchCombo.selectedItem = currentBranch
        }
        
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.border = JBUI.Borders.empty(10)
        panel.preferredSize = Dimension(450, 200)
        
        val formPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("源分支 (你的分支):"))
            })
            add(sourceBranchCombo)
            add(Box.createVerticalStrut(15))
            
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("目标分支 (比对基准):"))
            })
            add(targetBranchCombo)
            add(Box.createVerticalStrut(20))
            
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("<html><body style='width: 400px; color: gray;'>将检测源分支相对于目标分支的所有变更，<br>并分析受影响的流量入口。</body></html>"))
            })
        }
        
        panel.add(formPanel, BorderLayout.CENTER)
        
        return panel
    }

    override fun doOKAction() {
        sourceBranch = sourceBranchCombo.selectedItem as? String
        targetBranch = targetBranchCombo.selectedItem as? String
        
        if (sourceBranch.isNullOrBlank() || targetBranch.isNullOrBlank()) {
            JOptionPane.showMessageDialog(
                contentPane,
                "请选择源分支和目标分支",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        if (sourceBranch == targetBranch) {
            JOptionPane.showMessageDialog(
                contentPane,
                "源分支和目标分支不能相同",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        super.doOKAction()
    }
    
    companion object {
        fun show(
            project: Project,
            branches: List<String>,
            currentBranch: String?
        ): Pair<String, String>? {
            val dialog = BranchDiffDialog(project, branches, currentBranch)
            if (dialog.showAndGet()) {
                return dialog.sourceBranch?.let { src ->
                    dialog.targetBranch?.let { tgt ->
                        src to tgt
                    }
                }
            }
            return null
        }
    }
}