package org.ripple.endpoint.ui.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.ripple.endpoint.core.model.DetectionSummary

object EndpointNotifier {
    
    private const val NOTIFICATION_GROUP = "Ripple"
    
    fun notifyDetectionStarted(project: Project) {
        notify(project, "流量入口检测中...", NotificationType.INFORMATION)
    }
    
    fun notifyDetectionComplete(project: Project, summary: DetectionSummary) {
        val message = buildString {
            append("检测完成: 共 ${summary.affectedEntries} 个受影响入口")
            val highCount = summary.byImpact.filter { it.key.name == "HIGH" }.values.sum()
            if (highCount > 0) {
                append(", $highCount 个高风险")
            }
        }
        notify(project, message, NotificationType.INFORMATION)
    }
    
    fun notifyHighRisk(project: Project, count: Int) {
        notify(
            project,
            "发现高风险变更! 检测到 $count 个高风险流量入口受影响",
            NotificationType.WARNING
        )
    }
    
    fun notifyNoChanges(project: Project) {
        notify(project, "当前无 Git 变更文件，无需检测", NotificationType.INFORMATION)
    }
    
    fun notifyError(project: Project, message: String) {
        notify(project, "检测失败: $message", NotificationType.ERROR)
    }
    
    fun notifyInfo(project: Project, message: String) {
        notify(project, message, NotificationType.INFORMATION)
    }
    
    private fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(content, type)
            .notify(project)
    }
}