package org.ripple.endpoint.plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.ripple.endpoint.report.generator.MarkdownReportGenerator
import org.ripple.endpoint.report.generator.ReportGeneratorFactory
import org.ripple.endpoint.ui.notification.EndpointNotifier
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GenerateReportAction : AnAction("生成检测报告", "生成流量入口检测报告", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val reportDir = Path.of(project.basePath ?: return, "flow-detector-report")
            Files.createDirectories(reportDir)
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val reportFile = reportDir.resolve("detection-report-$timestamp.md")
            
            val generator = ReportGeneratorFactory.create(
                org.ripple.endpoint.report.generator.ReportFormat.MARKDOWN
            )
            
            val sampleReport = createSampleReport(project.name)
            val content = generator.generate(sampleReport)
            
            Files.writeString(reportFile, content)
            
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(reportFile.toFile())
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
            
            EndpointNotifier.notifyDetectionComplete(
                project,
                org.ripple.endpoint.core.model.DetectionSummary(
                    totalEntries = 0,
                    affectedEntries = 0,
                    byType = emptyMap(),
                    byImpact = emptyMap()
                )
            )
            
        } catch (e: Exception) {
            EndpointNotifier.notifyError(project, e.message ?: "Unknown error")
        }
    }
    
    private fun createSampleReport(projectName: String): org.ripple.endpoint.report.model.DetectionReport {
        return org.ripple.endpoint.report.model.DetectionReport(
            metadata = org.ripple.endpoint.report.model.ReportMetadata(
                projectName = projectName,
                generatedAt = LocalDateTime.now().toString(),
                pluginVersion = "1.0.0"
            ),
            changeSummary = org.ripple.endpoint.report.model.ChangeSummary(
                changeType = org.ripple.endpoint.report.model.ChangeContextType.WORKING,
                changedFiles = emptyList(),
                totalChanges = 0
            ),
            affectedEntries = emptyList(),
            callGraph = null,
            statistics = org.ripple.endpoint.report.model.ReportStatistics(
                totalEntries = 0,
                affectedEntries = 0,
                unaffectedEntries = 0,
                byType = emptyMap(),
                byImpact = emptyMap()
            )
        )
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}