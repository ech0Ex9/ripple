package org.ripple.endpoint.core.model

import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.ImpactLevel

data class GlobalConfig(
    val detectors: Map<EntryType, DetectorConfig>,
    val general: GeneralConfig
)

data class GeneralConfig(
    val maxTraceDepth: Int = 20,
    val reportOutputPath: String = "flow-detector-report",
    val commitCheckEnabled: Boolean = true,
    val highRiskBlockCommit: Boolean = true
)

data class SensitivePattern(
    val pattern: String,
    val entryTypes: List<EntryType>? = null,
    val reason: String,
    val risk: ImpactLevel = ImpactLevel.HIGH
)

data class IgnoreRule(
    val pattern: String,
    val reason: String
)

data class RiskThresholds(
    val blockCommitOn: Set<ImpactLevel> = setOf(ImpactLevel.HIGH),
    val notifyOn: Set<ImpactLevel> = setOf(ImpactLevel.MEDIUM, ImpactLevel.LOW)
)

data class SharedConfig(
    val version: String = "1.0",
    val sensitivePatterns: List<SensitivePattern> = emptyList(),
    val ignoreRules: List<IgnoreRule> = emptyList(),
    val riskThresholds: RiskThresholds = RiskThresholds(),
    val commitCheckEnabled: Boolean? = null
)