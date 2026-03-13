package org.ripple.endpoint.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.ripple.endpoint.core.engine.DefaultDetectionEngine
import org.ripple.endpoint.core.engine.DetectionRequest
import org.ripple.endpoint.core.git.DefaultGitChangeResolver
import org.ripple.endpoint.detector.grpc.GrpcDetector
import org.ripple.endpoint.detector.mq.MqDetector
import org.ripple.endpoint.detector.scheduled.ScheduledDetector
import org.ripple.endpoint.detector.spring.SpringMvcDetector
import org.ripple.endpoint.ui.notification.EndpointNotifier

class DetectChangesAction : AnAction("检测流量入口变更", "检测当前变更影响的流量入口", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectPath = project.basePath ?: return
        
        EndpointNotifier.notifyDetectionStarted(project)
        
        try {
            val gitResolver = DefaultGitChangeResolver()
            val workingChanges = gitResolver.resolveWorkingChanges(projectPath)
            
            if (workingChanges.isEmpty()) {
                EndpointNotifier.notifyNoChanges(project)
                return
            }
            
            val detectors = listOf(
                SpringMvcDetector(),
                GrpcDetector(),
                MqDetector(),
                ScheduledDetector()
            )
            
            val engine = DefaultDetectionEngine(detectors)
            
            val config = detectors.associate { detector ->
                detector.name to detector.defaultConfig
            }
            
            val request = DetectionRequest(
                projectPath = projectPath,
                changedFiles = workingChanges,
                config = config
            )
            
            val response = engine.detect(request)
            
            EndpointNotifier.notifyDetectionComplete(project, response.summary)
            
            updateToolWindow(project, response.results, response.summary)
            
        } catch (ex: Exception) {
            EndpointNotifier.notifyError(project, ex.message ?: "Unknown error")
            ex.printStackTrace()
        }
    }
    
    private fun updateToolWindow(
        project: Project, 
        results: List<org.ripple.endpoint.core.model.DetectionResult>,
        summary: org.ripple.endpoint.core.model.DetectionSummary
    ) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("Endpoint Detector")
        
        if (toolWindow != null) {
            toolWindow.activate {
                val contentManager = toolWindow.contentManager
                val content = contentManager.getContent(0)
                if (content != null) {
                    val panel = content.component as? org.ripple.endpoint.ui.toolwindow.ResultTreePanel
                    panel?.updateResults(results, summary)
                }
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}