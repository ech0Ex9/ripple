package org.ripple.endpoint.detector.api

import org.ripple.endpoint.detector.api.model.EntryType

/**
 * 检测器配置
 */
data class DetectorConfig(
    val enabled: Boolean = true,
    val annotations: List<String> = emptyList(),
    val basePackages: List<String> = emptyList(),
    val customRules: List<CustomRule> = emptyList()
)

/**
 * 自定义规则
 */
data class CustomRule(
    val name: String,
    val annotation: String,
    val basePackage: String,
    val entryType: EntryType = EntryType.CUSTOM
)