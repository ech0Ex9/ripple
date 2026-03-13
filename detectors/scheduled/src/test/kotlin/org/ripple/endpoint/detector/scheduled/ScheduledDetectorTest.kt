package org.ripple.endpoint.detector.scheduled

import org.ripple.endpoint.detector.api.model.EntryType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class ScheduledDetectorTest {
    
    private val detector = ScheduledDetector()
    
    @Test
    fun `should detect cron scheduled task`() {
        val sourceCode = """
            package com.example.task;
            
            import org.springframework.scheduling.annotation.Scheduled;
            
            public class ScheduledTasks {
                @Scheduled(cron = "0 0 2 * * ?")
                public void nightlyCleanup() {
                    cleanupOldData();
                }
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "src/main/java/com/example/task/ScheduledTasks.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertEquals(EntryType.SCHEDULED, results[0].type)
        assertEquals("nightlyCleanup", results[0].name)
        assertTrue(results[0].path?.contains("cron") == true)
    }
    
    @Test
    fun `should detect fixedRate scheduled task`() {
        val sourceCode = """
            @Scheduled(fixedRate = 5000)
            public void periodicCheck() {
                checkStatus();
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "task/HealthTask.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].path?.contains("fixedRate") == true)
    }
    
    @Test
    fun `should detect multiple scheduled methods`() {
        val sourceCode = """
            public class MultiTask {
                @Scheduled(cron = "0 0 1 * * ?")
                public void task1() {}
                
                @Scheduled(fixedRate = 10000)
                public void task2() {}
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "task/MultiTask.java", detector.defaultConfig)
        
        assertEquals(2, results.size)
    }
}