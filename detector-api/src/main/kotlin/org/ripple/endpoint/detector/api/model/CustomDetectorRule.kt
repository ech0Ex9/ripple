package org.ripple.endpoint.detector.api.model

/**
 * 自定义检测规则
 * 
 * 用于配置用户自定义的流量入口检测规则
 */
data class CustomDetectorRule(
    val id: String,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    
    // 匹配规则
    val filePatterns: List<String> = emptyList(),      // 文件匹配模式，如 "**/rpc/**/*.java"
    val annotations: List<String> = emptyList(),        // 注解匹配，如 "@RpcService", "@DubboService"
    val classNames: List<String> = emptyList(),         // 类名匹配，如 "*ServiceImpl", "*Provider"
    val methodPatterns: List<String> = emptyList(),     // 方法模式，如 "public * *(*)"
    
    // 提取规则
    val pathExtraction: PathExtractionRule? = null,     // 路径提取规则
    val nameExtraction: NameExtractionRule? = null,     // 名称提取规则
    
    // 入口类型
    val entryType: EntryType = EntryType.CUSTOM,
    
    // 元数据
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 路径提取规则
 */
data class PathExtractionRule(
    val type: PathExtractionType,
    val pattern: String = "",           // 正则表达式
    val annotationAttr: String = "",    // 注解属性名，如 "path", "value"
    val defaultValue: String = ""
)

enum class PathExtractionType {
    FROM_ANNOTATION,    // 从注解属性提取
    FROM_CLASS_NAME,    // 从类名提取
    FROM_METHOD_NAME,   // 从方法名提取
    FROM_REGEX,         // 正则表达式提取
    FIXED_VALUE         // 固定值
}

/**
 * 名称提取规则
 */
data class NameExtractionRule(
    val type: NameExtractionType,
    val pattern: String = ""
)

enum class NameExtractionType {
    METHOD_NAME,        // 使用方法名
    CLASS_NAME,         // 使用类名
    ANNOTATION_VALUE,   // 注解值
    REGEX_GROUP         // 正则捕获组
}

/**
 * 规则配置状态（用于持久化）
 */
data class DetectorRuleState(
    var rules: List<CustomDetectorRule> = emptyList()
) {
    companion object {
        val DEFAULT_RULES = listOf(
            CustomDetectorRule(
                id = "custom-dubbo",
                name = "Dubbo Service",
                description = "检测 Dubbo 服务提供者",
                filePatterns = listOf("**/dubbo/**/*.java", "**/provider/**/*.java"),
                annotations = listOf("org.apache.dubbo.config.annotation.Service", 
                                    "com.alibaba.dubbo.config.annotation.Service"),
                classNames = listOf("*ServiceImpl", "*Provider"),
                entryType = EntryType.CUSTOM,
                pathExtraction = PathExtractionRule(
                    type = PathExtractionType.FROM_CLASS_NAME,
                    pattern = "(.+)ServiceImpl",
                    defaultValue = "/dubbo"
                )
            ),
            CustomDetectorRule(
                id = "custom-feign",
                name = "Feign Client",
                description = "检测 Feign 客户端",
                filePatterns = listOf("**/feign/**/*.java", "**/client/**/*.java"),
                annotations = listOf("org.springframework.cloud.openfeign.FeignClient"),
                classNames = listOf("*Client", "*FeignClient"),
                entryType = EntryType.HTTP
            )
        )
    }
}