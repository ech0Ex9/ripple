package org.ripple.endpoint.detector.api.model

/**
 * 变更文件
 */
data class ChangedFile(
    val path: String,
    val changeType: ChangeType,
    val linesAdded: Int = 0,
    val linesDeleted: Int = 0,
    val content: String? = null
)