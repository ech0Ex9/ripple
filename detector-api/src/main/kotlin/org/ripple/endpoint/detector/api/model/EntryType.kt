package org.ripple.endpoint.detector.api.model

/**
 * 流量入口类型
 */
enum class EntryType {
    HTTP,
    GRPC,
    MQ,
    SCHEDULED,
    CUSTOM
}