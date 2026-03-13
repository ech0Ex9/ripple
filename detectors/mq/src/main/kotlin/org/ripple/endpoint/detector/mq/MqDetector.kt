package org.ripple.endpoint.detector.mq

import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.TrafficDetector
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.TrafficEntry

class MqDetector : TrafficDetector {
    
    override val entryType = EntryType.MQ
    override val name = "MQ"
    override val description = "Detects message queue consumers (RocketMQ, Kafka, RabbitMQ, etc.)"
    
    override val defaultConfig = DetectorConfig(
        enabled = true,
        annotations = DEFAULT_ANNOTATIONS,
        basePackages = DEFAULT_PACKAGES
    )
    
    override fun detect(
        sourceCode: String,
        filePath: String,
        config: DetectorConfig
    ): List<TrafficEntry> {
        if (!filePath.endsWith(".java") && !filePath.endsWith(".kt")) {
            return emptyList()
        }
        
        val entries = mutableListOf<TrafficEntry>()
        val annotations = config.annotations.ifEmpty { DEFAULT_ANNOTATIONS }
        
        annotations.forEach { annotation ->
            when {
                annotation.contains("RocketMQ", ignoreCase = true) -> 
                    detectRocketMq(sourceCode, filePath, annotation, entries)
                annotation.contains("Kafka", ignoreCase = true) -> 
                    detectKafka(sourceCode, filePath, annotation, entries)
                annotation.contains("Rabbit", ignoreCase = true) || annotation.contains("Listener", ignoreCase = true) -> 
                    detectGenericMq(sourceCode, filePath, annotation, entries)
                else -> 
                    detectGenericMq(sourceCode, filePath, annotation, entries)
            }
        }
        
        return entries.distinctBy { it.id }
    }
    
    private fun detectRocketMq(
        sourceCode: String, 
        filePath: String, 
        annotation: String,
        entries: MutableList<TrafficEntry>
    ) {
        val annotationName = annotation.substringAfterLast('.')
        val pattern = Regex(
            """@$annotationName\s*\(([^)]+)\)""",
            RegexOption.DOT_MATCHES_ALL
        )
        val classPattern = Regex(
            """@$annotationName[^@]*?(?:public\s+)?class\s+(\w+)""",
            RegexOption.DOT_MATCHES_ALL
        )
        
        classPattern.find(sourceCode)?.let { classMatch ->
            val className = classMatch.groupValues[1]
            val annotationContent = pattern.find(sourceCode)?.groupValues?.get(1) ?: ""
            
            val topic = extractValue(annotationContent, "topic")
            val consumerGroup = extractValue(annotationContent, "consumerGroup")
            val selectorExpression = extractValue(annotationContent, "selectorExpression").ifEmpty { "*" }
            
            val lineNumber = sourceCode.substring(0, classMatch.range.first).lines().size
            
            entries.add(TrafficEntry(
                type = EntryType.MQ,
                name = className,
                path = "$topic:$selectorExpression",
                containingFile = filePath,
                line = lineNumber,
                annotations = listOf(annotation),
                metadata = mapOf(
                    "topic" to topic,
                    "tags" to selectorExpression,
                    "consumerGroup" to consumerGroup,
                    "mqType" to "RocketMQ"
                )
            ))
        }
    }
    
    private fun detectKafka(
        sourceCode: String, 
        filePath: String, 
        annotation: String,
        entries: MutableList<TrafficEntry>
    ) {
        val annotationName = annotation.substringAfterLast('.')
        val pattern = Regex(
            """@$annotationName\s*\(([^)]+)\)""",
            RegexOption.DOT_MATCHES_ALL
        )
        val methodPattern = Regex(
            """@$annotationName\s*\([^)]*\)\s*(?:public\s+)?(?:\w+(?:<[^>]+>)?\s+)?(\w+)\s*\(""",
            RegexOption.DOT_MATCHES_ALL
        )
        
        methodPattern.findAll(sourceCode).forEach { match ->
            val methodName = match.groupValues[1]
            val annotationContent = pattern.find(sourceCode.substring(0, match.range.last))?.groupValues?.get(1) ?: ""
            
            val topics = extractValue(annotationContent, "topics")
            val groupId = extractValue(annotationContent, "groupId")
            
            val lineNumber = sourceCode.substring(0, match.range.first).lines().size
            
            entries.add(TrafficEntry(
                type = EntryType.MQ,
                name = methodName,
                path = topics.ifEmpty { "unknown-topic" },
                containingFile = filePath,
                line = lineNumber,
                annotations = listOf(annotation),
                metadata = mapOf(
                    "topics" to topics,
                    "groupId" to groupId,
                    "mqType" to "Kafka"
                )
            ))
        }
    }
    
    private fun detectGenericMq(
        sourceCode: String, 
        filePath: String, 
        annotation: String,
        entries: MutableList<TrafficEntry>
    ) {
        val annotationName = annotation.substringAfterLast('.')
        val pattern = Regex(
            """@$annotationName\s*(?:\([^)]*\))?\s*(?:public\s+)?(?:class\s+(\w+)|(?:\w+(?:<[^>]+>)?\s+)?(\w+)\s*\()""",
            RegexOption.DOT_MATCHES_ALL
        )
        
        pattern.findAll(sourceCode).forEach { match ->
            val name = match.groupValues[1].ifEmpty { match.groupValues[2] }
            if (name.isNotEmpty()) {
                val lineNumber = sourceCode.substring(0, match.range.first).lines().size
                
                entries.add(TrafficEntry(
                    type = EntryType.MQ,
                    name = name,
                    path = "message-consumer",
                    containingFile = filePath,
                    line = lineNumber,
                    annotations = listOf(annotation),
                    metadata = mapOf("mqType" to "Generic")
                ))
            }
        }
    }
    
    override fun isApplicable(filePath: String, config: DetectorConfig): Boolean {
        val packages = config.basePackages.ifEmpty { DEFAULT_PACKAGES }
        return packages.any { pkg -> 
            filePath.contains("/$pkg/") || filePath.contains("\\$pkg\\")
        } && (filePath.endsWith(".java") || filePath.endsWith(".kt"))
    }
    
    private fun extractValue(content: String, key: String): String {
        val stringPattern = Regex("""$key\s*=\s*["']([^"']+)["']""")
        val arrayPattern = Regex("""$key\s*=\s*\{([^}]+)\}""")
        
        return stringPattern.find(content)?.groupValues?.get(1)
            ?: arrayPattern.find(content)?.groupValues?.get(1)?.replace("\"", "")?.trim()
            ?: ""
    }
    
    companion object {
        val DEFAULT_ANNOTATIONS = listOf(
            "org.apache.rocketmq.spring.annotation.RocketMQMessageListener",
            "org.springframework.kafka.annotation.KafkaListener",
            "org.springframework.amqp.rabbit.annotation.RabbitListener",
            "org.springframework.jms.annotation.JmsListener"
        )
        val DEFAULT_PACKAGES = listOf("mq", "consumer", "listener", "message", "queue")
    }
}