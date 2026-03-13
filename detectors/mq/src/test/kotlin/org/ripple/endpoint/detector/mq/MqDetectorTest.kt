package org.ripple.endpoint.detector.mq

import org.ripple.endpoint.detector.api.model.EntryType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class MqDetectorTest {
    
    private val detector = MqDetector()
    
    @Test
    fun `should detect RocketMQ consumer`() {
        val sourceCode = """
            package com.example.mq;
            
            import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
            
            @RocketMQMessageListener(
                topic = "order-created",
                consumerGroup = "order-group",
                selectorExpression = "tag1 || tag2"
            )
            public class OrderConsumer implements RocketMQListener<Order> {
                public void onMessage(Order order) {
                    processOrder(order);
                }
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "src/main/java/com/example/mq/OrderConsumer.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertEquals(EntryType.MQ, results[0].type)
        assertTrue(results[0].path?.contains("order-created") == true)
    }
    
    @Test
    fun `should detect Kafka listener`() {
        val sourceCode = """
            package com.example.kafka;
            
            import org.springframework.kafka.annotation.KafkaListener;
            
            public class KafkaConsumer {
                @KafkaListener(topics = "user-events", groupId = "user-group")
                public void consume(String message) {
                    process(message);
                }
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "src/main/java/com/example/kafka/KafkaConsumer.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertEquals(EntryType.MQ, results[0].type)
    }
    
    @Test
    fun `should extract topic and tags from RocketMQ`() {
        val sourceCode = """
            @RocketMQMessageListener(topic = "test-topic", selectorExpression = "tagA")
            public class TestConsumer {}
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "mq/TestConsumer.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].path?.contains("test-topic:tagA") == true)
    }
}