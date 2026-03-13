package org.ripple.endpoint.report.generator

import org.ripple.endpoint.report.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonReportGenerator : ReportGenerator {
    
    private val json = Json { prettyPrint = true }
    
    override val format = ReportFormat.JSON
    override val fileExtension = "json"
    
    override fun generate(report: DetectionReport): String {
        return json.encodeToString(report)
    }
}

class HtmlReportGenerator : ReportGenerator {
    
    override val format = ReportFormat.HTML
    override val fileExtension = "html"
    
    override fun generate(report: DetectionReport): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"zh-CN\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<title>流量入口检测报告 - ${report.metadata.projectName}</title>")
            appendLine("<style>")
            appendLine("body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; margin: 40px; }")
            appendLine("table { border-collapse: collapse; width: 100%; margin: 20px 0; }")
            appendLine("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            appendLine("th { background-color: #f5f5f5; }")
            appendLine(".high { color: #dc3545; }")
            appendLine(".medium { color: #ffc107; }")
            appendLine(".low { color: #28a745; }")
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<h1>流量入口检测报告</h1>")
            appendLine("<p>项目: ${report.metadata.projectName}</p>")
            appendLine("<p>生成时间: ${report.metadata.generatedAt}</p>")
            appendLine("<h2>统计</h2>")
            appendLine("<p>总入口数: ${report.statistics.totalEntries}</p>")
            appendLine("<p>受影响入口: ${report.statistics.affectedEntries}</p>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }
}