package org.ripple.endpoint.report.generator

import org.ripple.endpoint.report.model.*

class MarkdownReportGenerator : ReportGenerator {
    
    override val format = ReportFormat.MARKDOWN
    override val fileExtension = "md"
    
    override fun generate(report: DetectionReport): String {
        return buildString {
            appendHeader(report)
            appendChangeSummary(report.changeSummary)
            appendAffectedEntries(report.affectedEntries)
            appendStatistics(report.statistics)
            appendFooter()
        }
    }
    
    private fun StringBuilder.appendHeader(report: DetectionReport) {
        appendLine("# 流量入口检测报告")
        appendLine()
        appendLine("| 项目 | ${report.metadata.projectName} |")
        appendLine("|------|------|")
        appendLine("| 生成时间 | ${report.metadata.generatedAt} |")
        appendLine("| 插件版本 | ${report.metadata.pluginVersion} |")
        report.changeSummary.sourceBranch?.let {
            appendLine("| 源分支 | $it |")
        }
        report.changeSummary.targetBranch?.let {
            appendLine("| 目标分支 | $it |")
        }
        appendLine()
    }
    
    private fun StringBuilder.appendChangeSummary(summary: ChangeSummary) {
        appendLine("## 📁 变更概览")
        appendLine()
        appendLine("共 **${summary.totalChanges}** 个文件变更")
        appendLine()
        appendLine("| 文件路径 | 变更类型 | +行 | -行 |")
        appendLine("|----------|----------|-----|-----|")
        summary.changedFiles.forEach { file ->
            val changeIcon = when (file.changeType) {
                org.ripple.endpoint.detector.api.model.ChangeType.ADD -> "🟢 新增"
                org.ripple.endpoint.detector.api.model.ChangeType.MODIFY -> "🟡 修改"
                org.ripple.endpoint.detector.api.model.ChangeType.DELETE -> "🔴 删除"
            }
            appendLine("| `${file.path}` | $changeIcon | +${file.linesAdded} | -${file.linesDeleted} |")
        }
        appendLine()
    }
    
    private fun StringBuilder.appendAffectedEntries(entries: List<AffectedEntry>) {
        appendLine("## 🎯 受影响的流量入口")
        appendLine()
        
        val groupedByImpact = entries.groupBy { it.impactLevel }
        
        val highRisk = groupedByImpact["HIGH"] ?: emptyList()
        if (highRisk.isNotEmpty()) {
            appendLine("### 🔴 高风险 (${highRisk.size})")
            appendLine()
            highRisk.forEach { appendAffectedEntry(it) }
        }
        
        val mediumRisk = groupedByImpact["MEDIUM"] ?: emptyList()
        if (mediumRisk.isNotEmpty()) {
            appendLine("### 🟡 中风险 (${mediumRisk.size})")
            appendLine()
            mediumRisk.forEach { appendAffectedEntry(it) }
        }
        
        val lowRisk = groupedByImpact["LOW"] ?: emptyList()
        if (lowRisk.isNotEmpty()) {
            appendLine("### 🟢 低风险 (${lowRisk.size})")
            appendLine()
            lowRisk.forEach { appendAffectedEntry(it) }
        }
    }
    
    private fun StringBuilder.appendAffectedEntry(entry: AffectedEntry) {
        val typeIcon = when (entry.entryType) {
            "HTTP" -> "🌐"
            "GRPC" -> "⚡"
            "MQ" -> "📨"
            "SCHEDULED" -> "⏰"
            else -> "🔧"
        }
        
        appendLine("#### $typeIcon ${entry.entryName}")
        appendLine()
        appendLine("- **类型**: ${entry.entryType}")
        appendLine("- **路径**: `${entry.entryPath ?: "N/A"}`")
        appendLine("- **文件**: `${entry.containingFile}:${entry.line}`")
        appendLine("- **影响原因**: ${entry.impactReason}")
        
        if (entry.callPath.isNotEmpty()) {
            appendLine("- **调用链路**:")
            entry.callPath.forEach { node ->
                appendLine("  - $node")
            }
        }
        
        appendLine("- **关联变更**: ${entry.changedFiles.joinToString(", ") { "`$it`" }}")
        appendLine()
    }
    
    private fun StringBuilder.appendStatistics(stats: ReportStatistics) {
        appendLine("## 📊 统计信息")
        appendLine()
        appendLine("| 指标 | 数量 |")
        appendLine("|------|------|")
        appendLine("| 总入口数 | ${stats.totalEntries} |")
        appendLine("| 受影响入口 | ${stats.affectedEntries} |")
        appendLine("| 未受影响入口 | ${stats.unaffectedEntries} |")
        appendLine()
        
        appendLine("### 按风险级别")
        appendLine()
        appendLine("| 风险级别 | 数量 |")
        appendLine("|----------|------|")
        stats.byImpact.forEach { (level, count) ->
            val icon = when (level) {
                "HIGH" -> "🔴"
                "MEDIUM" -> "🟡"
                "LOW" -> "🟢"
                else -> "⚪"
            }
            appendLine("| $icon $level | $count |")
        }
        appendLine()
    }
    
    private fun StringBuilder.appendFooter() {
        appendLine("---")
        appendLine()
        appendLine("*此报告由 IDEA 流量入口检测插件自动生成*")
    }
}