package org.ripple.endpoint.plugin.commit

import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.project.Project

class EndpointCheckinHandlerFactory : CheckinHandlerFactory() {
    
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return EndpointCheckinHandler(panel.project, panel)
    }
}

class EndpointCheckinHandler(
    private val project: Project,
    private val panel: CheckinProjectPanel
) : CheckinHandler() {
    
    override fun beforeCheckin(): ReturnResult {
        val projectPath = project.basePath ?: return ReturnResult.COMMIT
        
        val configEnabled = true
        
        if (!configEnabled) {
            return ReturnResult.COMMIT
        }
        
        val service = org.ripple.endpoint.plugin.service.DetectionService.getInstance(project)
        val gitResolver = service.getGitResolver()
        val engine = service.getEngine()
        
        val stagedChanges = gitResolver.resolveStagedChanges(projectPath)
        
        if (stagedChanges.isEmpty()) {
            return ReturnResult.COMMIT
        }
        
        val config = service.getDetectors().associate { it.name to it.defaultConfig }
        
        val request = org.ripple.endpoint.core.engine.DetectionRequest(
            projectPath = projectPath,
            changedFiles = stagedChanges,
            config = config
        )
        
        val response = engine.detect(request)
        
        org.ripple.endpoint.ui.notification.EndpointNotifier.notifyDetectionComplete(project, response.summary)
        
        val highRiskCount = response.summary.byImpact[org.ripple.endpoint.detector.api.model.ImpactLevel.HIGH] ?: 0
        
        if (highRiskCount > 0) {
            val result = showRiskDialog(highRiskCount, response.results.size)
            return if (result) ReturnResult.COMMIT else ReturnResult.CANCEL
        }
        
        return ReturnResult.COMMIT
    }
    
    private fun showRiskDialog(highRiskCount: Int, totalCount: Int): Boolean {
        val dialog = RiskConfirmationDialog(project, highRiskCount, totalCount)
        return dialog.showAndGet()
    }
}

class RiskConfirmationDialog(
    project: Project,
    private val highRiskCount: Int,
    private val totalCount: Int
) : com.intellij.openapi.ui.DialogWrapper(project) {
    
    init {
        title = "高风险变更警告"
        setOKButtonText("继续提交")
        setCancelButtonText("取消提交")
        init()
    }
    
    override fun createCenterPanel(): javax.swing.JComponent {
        return com.intellij.util.ui.FormBuilder.createFormBuilder()
            .addComponent(
                com.intellij.ui.components.JBLabel(
                    "<html><b>检测到 $highRiskCount 个高风险流量入口受影响</b></html>"
                )
            )
            .addComponent(
                com.intellij.ui.components.JBLabel(
                    "共 $totalCount 个受影响入口，请确认是否继续提交？"
                )
            )
            .panel
    }
}