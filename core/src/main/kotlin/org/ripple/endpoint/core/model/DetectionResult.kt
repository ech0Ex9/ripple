package org.ripple.endpoint.core.model

import org.ripple.endpoint.detector.api.model.ImpactLevel
import org.ripple.endpoint.detector.api.model.TrafficEntry

data class DetectionResult(
    val entry: TrafficEntry,
    val impactLevel: ImpactLevel,
    val impactReason: String,
    val changedFiles: List<String>,
    val callPath: List<CallGraphNode> = emptyList()
)

data class ImpactPath(
    val entry: TrafficEntry,
    val path: List<CallGraphNode>,
    val impactLevel: ImpactLevel
)

data class TypeStats(
    val total: Int,
    val affected: Int,
    val highRisk: Int
)

data class DetectionSummary(
    val totalEntries: Int,
    val affectedEntries: Int,
    val byType: Map<String, TypeStats>,
    val byImpact: Map<ImpactLevel, Int>
)