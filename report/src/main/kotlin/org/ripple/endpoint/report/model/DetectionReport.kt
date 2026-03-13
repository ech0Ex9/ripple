package org.ripple.endpoint.report.model

import org.ripple.endpoint.core.model.DetectionResult
import org.ripple.endpoint.core.model.DetectionSummary
import org.ripple.endpoint.core.model.CallGraph
import org.ripple.endpoint.detector.api.model.ChangeType

data class DetectionReport(
    val metadata: ReportMetadata,
    val changeSummary: ChangeSummary,
    val affectedEntries: List<AffectedEntry>,
    val callGraph: CallGraph?,
    val statistics: ReportStatistics
)

data class ReportMetadata(
    val projectName: String,
    val generatedAt: String,
    val pluginVersion: String,
    val configVersion: String? = null
)

data class ChangeSummary(
    val changeType: ChangeContextType,
    val sourceBranch: String? = null,
    val targetBranch: String? = null,
    val changedFiles: List<ChangedFileInfo>,
    val totalChanges: Int
)

enum class ChangeContextType {
    WORKING,
    STAGED,
    BRANCH_DIFF
}

data class ChangedFileInfo(
    val path: String,
    val changeType: ChangeType,
    val linesAdded: Int = 0,
    val linesDeleted: Int = 0
)

data class AffectedEntry(
    val entryType: String,
    val entryName: String,
    val entryPath: String?,
    val containingFile: String,
    val line: Int,
    val impactLevel: String,
    val impactReason: String,
    val callPath: List<String>,
    val changedFiles: List<String>
)

data class ReportStatistics(
    val totalEntries: Int,
    val affectedEntries: Int,
    val unaffectedEntries: Int,
    val byType: Map<String, TypeStatistics>,
    val byImpact: Map<String, Int>
)

data class TypeStatistics(
    val total: Int,
    val affected: Int,
    val highRisk: Int
)