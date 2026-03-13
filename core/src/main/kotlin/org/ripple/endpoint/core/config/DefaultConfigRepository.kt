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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class DefaultConfigRepository : ConfigRepository {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    override fun load(projectPath: String): MergedConfig {
        val global = loadGlobalConfig()
        val shared = loadSharedConfig(projectPath)
        val project = loadProjectConfig(projectPath)
        
        val effective = mergeConfigs(global, shared, project)
        
        return MergedConfig(
            global = global,
            project = project,
            shared = shared,
            effective = effective
        )
    }
    
    override fun save(projectPath: String, config: MergedConfig, scope: ConfigScope) {
        when (scope) {
            ConfigScope.GLOBAL -> saveGlobalConfig(config.global)
            ConfigScope.PROJECT -> saveProjectConfig(projectPath, config.project)
            ConfigScope.SHARED -> saveSharedConfig(projectPath, config.shared)
        }
    }
    
    override fun reset(projectPath: String): MergedConfig {
        return load(projectPath)
    }
    
    private fun loadGlobalConfig(): GlobalConfig {
        val path = getGlobalConfigPath()
        if (!Files.exists(path)) {
            return createDefaultGlobalConfig()
        }
        
        return try {
            val content = Files.readString(path)
            json.decodeFromString(content)
        } catch (e: Exception) {
            createDefaultGlobalConfig()
        }
    }
    
    private fun loadSharedConfig(projectPath: String): SharedConfig? {
        val path = Path.of(projectPath, SHARED_CONFIG_FILE)
        if (!Files.exists(path)) {
            return null
        }
        
        return try {
            val content = Files.readString(path)
            json.decodeFromString(content)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun loadProjectConfig(projectPath: String): ProjectConfig? {
        val path = Path.of(projectPath, PROJECT_CONFIG_DIR, "config.json")
        if (!Files.exists(path)) {
            return null
        }
        
        return try {
            val content = Files.readString(path)
            json.decodeFromString(content)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun mergeConfigs(
        global: GlobalConfig,
        shared: SharedConfig?,
        project: ProjectConfig?
    ): EffectiveConfig {
        val sensitivePatterns = (shared?.sensitivePatterns ?: emptyList()) +
            global.general.let { emptyList<SensitivePattern>() }
        
        val ignoreRules = (shared?.ignoreRules ?: emptyList())
        
        val riskThresholds = shared?.riskThresholds ?: RiskThresholds()
        
        val detectors = global.detectors
        
        return EffectiveConfig(
            commitCheckEnabled = project?.commitCheckEnabled 
                ?: shared?.commitCheckEnabled 
                ?: global.general.commitCheckEnabled,
            highRiskBlockCommit = global.general.highRiskBlockCommit,
            sensitivePatterns = sensitivePatterns,
            ignoreRules = ignoreRules,
            riskThresholds = riskThresholds,
            detectors = detectors
        )
    }
    
    private fun saveGlobalConfig(config: GlobalConfig) {
        val path = getGlobalConfigPath()
        Files.createDirectories(path.parent)
        Files.writeString(path, json.encodeToString(config))
    }
    
    private fun saveSharedConfig(projectPath: String, config: SharedConfig?) {
        if (config == null) return
        val path = Path.of(projectPath, SHARED_CONFIG_FILE)
        Files.writeString(path, json.encodeToString(config))
    }
    
    private fun saveProjectConfig(projectPath: String, config: ProjectConfig?) {
        if (config == null) return
        val dir = Path.of(projectPath, PROJECT_CONFIG_DIR)
        Files.createDirectories(dir)
        Files.writeString(dir.resolve("config.json"), json.encodeToString(config))
    }
    
    private fun getGlobalConfigPath(): Path {
        val home = System.getProperty("user.home")
        return Path.of(home, ".config", CONFIG_DIR_NAME, "config.json")
    }
    
    private fun createDefaultGlobalConfig(): GlobalConfig {
        return GlobalConfig(
            detectors = mapOf(
                EntryType.HTTP to DetectorConfig(
                    enabled = true,
                    annotations = listOf(
                        "org.springframework.web.bind.annotation.RequestMapping",
                        "org.springframework.web.bind.annotation.GetMapping",
                        "org.springframework.web.bind.annotation.PostMapping"
                    ),
                    basePackages = listOf("controller", "api")
                ),
                EntryType.GRPC to DetectorConfig(
                    enabled = true,
                    annotations = listOf("io.grpc.stub.annotations.GrpcService"),
                    basePackages = listOf("grpc")
                ),
                EntryType.MQ to DetectorConfig(
                    enabled = true,
                    annotations = listOf("org.apache.rocketmq.spring.annotation.RocketMQMessageListener"),
                    basePackages = listOf("mq", "consumer")
                ),
                EntryType.SCHEDULED to DetectorConfig(
                    enabled = true,
                    annotations = listOf("org.springframework.scheduling.annotation.Scheduled"),
                    basePackages = listOf("task", "job")
                )
            ),
            general = GeneralConfig()
        )
    }
    
    companion object {
        private const val CONFIG_DIR_NAME = "idea-endpoint-detector"
        private const val SHARED_CONFIG_FILE = ".endpoint-detector.json"
        private const val PROJECT_CONFIG_DIR = ".idea/endpoint-detector"
    }
}

@Serializable
data class SharedConfigDto(
    val version: String = "1.0",
    val sensitivePatterns: List<SensitivePatternDto> = emptyList(),
    val ignoreRules: List<IgnoreRuleDto> = emptyList(),
    val riskThresholds: RiskThresholdsDto? = null,
    val commitCheckEnabled: Boolean? = null
)

@Serializable
data class SensitivePatternDto(
    val pattern: String,
    val entryTypes: List<String>? = null,
    val reason: String,
    val risk: String = "HIGH"
)

@Serializable
data class IgnoreRuleDto(
    val pattern: String,
    val reason: String
)

@Serializable
data class RiskThresholdsDto(
    val blockCommitOn: List<String> = listOf("HIGH"),
    val notifyOn: List<String> = listOf("MEDIUM", "LOW")
)