package org.ripple.endpoint.detector.api

import org.ripple.endpoint.detector.api.model.ChangedFile

/**
 * 检测上下文
 */
data class DetectionContext(
    val projectPath: String,
    val changedFiles: List<ChangedFile>,
    val config: Map<String, DetectorConfig>
)