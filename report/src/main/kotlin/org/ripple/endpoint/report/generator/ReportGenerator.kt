package org.ripple.endpoint.report.generator

import org.ripple.endpoint.report.model.*

interface ReportGenerator {
    val format: ReportFormat
    val fileExtension: String
    fun generate(report: DetectionReport): String
}

enum class ReportFormat {
    MARKDOWN,
    JSON,
    HTML
}

object ReportGeneratorFactory {
    fun create(format: ReportFormat): ReportGenerator {
        return when (format) {
            ReportFormat.MARKDOWN -> MarkdownReportGenerator()
            ReportFormat.JSON -> JsonReportGenerator()
            ReportFormat.HTML -> HtmlReportGenerator()
        }
    }
}