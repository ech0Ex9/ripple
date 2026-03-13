package org.ripple.endpoint.core.config

import org.ripple.endpoint.core.model.GeneralConfig
import org.ripple.endpoint.core.model.GlobalConfig
import org.ripple.endpoint.core.model.IgnoreRule
import org.ripple.endpoint.core.model.RiskThresholds
import org.ripple.endpoint.core.model.SensitivePattern
import org.ripple.endpoint.core.model.SharedConfig
import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.ImpactLevel
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ConfigScope {
    GLOBAL,
    PROJECT,
    SHARED
}

data class MergedConfig(
    val global: GlobalConfig,
    val project: ProjectConfig?,
    val shared: SharedConfig?,
    val effective: EffectiveConfig
)

data class ProjectConfig(
    val enabled: Boolean = true,
    val commitCheckEnabled: Boolean? = null,
    val overrideDetectors: Map<EntryType, DetectorConfig>? = null
)

data class EffectiveConfig(
    val commitCheckEnabled: Boolean,
    val highRiskBlockCommit: Boolean,
    val sensitivePatterns: List<SensitivePattern>,
    val ignoreRules: List<IgnoreRule>,
    val riskThresholds: RiskThresholds,
    val detectors: Map<EntryType, DetectorConfig>
)

interface ConfigRepository {
    fun load(projectPath: String): MergedConfig
    fun save(projectPath: String, config: MergedConfig, scope: ConfigScope)
    fun reset(projectPath: String): MergedConfig
}