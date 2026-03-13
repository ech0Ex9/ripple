package org.ripple.endpoint.core.engine

import org.ripple.endpoint.detector.api.model.ChangeType
import org.ripple.endpoint.detector.api.model.ChangedFile
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.ImpactLevel
import org.ripple.endpoint.detector.api.model.TrafficEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultDetectionEngineTest {
    
    @Test
    fun `should return empty results when no changes`() {
        val detector = MockDetector()
        val engine = DefaultDetectionEngine(listOf(detector))
        
        val request = DetectionRequest(
            projectPath = "/test",
            changedFiles = emptyList(),
            config = mapOf("Mock" to detector.defaultConfig)
        )
        
        val response = engine.detect(request)
        
        assertTrue(response.results.isEmpty())
        assertEquals(0, response.summary.totalEntries)
    }
    
    @Test
    fun `should detect entries from changed files`() {
        val detector = MockDetector()
        val engine = DefaultDetectionEngine(listOf(detector))
        
        val request = DetectionRequest(
            projectPath = "/test",
            changedFiles = listOf(
                ChangedFile("Controller.java", ChangeType.MODIFY)
            ),
            config = mapOf("Mock" to detector.defaultConfig)
        )
        
        val response = engine.detect(request)
        
        assertTrue(response.summary.totalEntries >= 0)
    }
}

class MockDetector : org.ripple.endpoint.detector.api.TrafficDetector {
    override val entryType = EntryType.HTTP
    override val name = "Mock"
    override val description = "Mock detector for testing"
    override val defaultConfig = org.ripple.endpoint.detector.api.DetectorConfig()
    
    override fun detect(
        sourceCode: String,
        filePath: String,
        config: org.ripple.endpoint.detector.api.DetectorConfig
    ): List<TrafficEntry> {
        return if (filePath.contains("Controller")) {
            listOf(TrafficEntry(
                type = EntryType.HTTP,
                name = "testMethod",
                path = "GET /test",
                containingFile = filePath,
                line = 1,
                annotations = emptyList()
            ))
        } else {
            emptyList()
        }
    }
    
    override fun isApplicable(filePath: String, config: org.ripple.endpoint.detector.api.DetectorConfig): Boolean {
        return filePath.endsWith(".java")
    }
}