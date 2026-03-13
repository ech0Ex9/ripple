package org.ripple.endpoint.detector.api.model

/**
 * 流量入口定义
 */
data class TrafficEntry(
    val type: EntryType,
    val name: String,
    val path: String?,
    val description: String? = null,      // 从方法注释提取的描述
    val containingFile: String,
    val line: Int,
    val annotations: List<String>,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 唯一标识
     */
    val id: String
        get() = "$containingFile:$name"
}