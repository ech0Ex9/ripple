package org.ripple.endpoint.core.engine

import org.ripple.endpoint.core.model.SensitivePattern
import org.ripple.endpoint.detector.api.model.ChangeType
import org.ripple.endpoint.detector.api.model.ChangedFile
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.ImpactLevel
import org.ripple.endpoint.detector.api.model.TrafficEntry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class ImpactAnalyzerTest {
    
    @Test
    fun `should return HIGH for deleted files`() {
        val analyzer = ImpactAnalyzer()
        val entry = createMockEntry()
        val changedFile = ChangedFile("Test.java", ChangeType.DELETE)
        
        val result = analyzer.analyze(entry, changedFile)
        
        assertEquals(ImpactLevel.HIGH, result)
    }
    
    @Test
    fun `should return MEDIUM for added files`() {
        val analyzer = ImpactAnalyzer()
        val entry = createMockEntry()
        val changedFile = ChangedFile("Test.java", ChangeType.ADD)
        
        val result = analyzer.analyze(entry, changedFile)
        
        assertEquals(ImpactLevel.MEDIUM, result)
    }
    
    @Test
    fun `should return HIGH for sensitive entry`() {
        val sensitivePatterns = listOf(
            SensitivePattern(
                pattern = "**/payment/**",
                reason = "Payment API"
            )
        )
        val analyzer = ImpactAnalyzer(sensitivePatterns)
        val entry = createMockEntry(path = "POST /api/payment/order")
        val changedFile = ChangedFile("Test.java", ChangeType.MODIFY)
        
        val result = analyzer.analyze(entry, changedFile)
        
        assertEquals(ImpactLevel.HIGH, result)
    }
    
    @Test
    fun `should return HIGH for signature change`() {
        val analyzer = ImpactAnalyzer()
        val entry = createMockEntry()
        val changedFile = ChangedFile("Test.java", ChangeType.MODIFY)
        val diff = FileDiff(hasSignatureChange = true)
        
        val result = analyzer.analyze(entry, changedFile, diff)
        
        assertEquals(ImpactLevel.HIGH, result)
    }
    
    private fun createMockEntry(path: String? = null): TrafficEntry {
        return TrafficEntry(
            type = EntryType.HTTP,
            name = "testMethod",
            path = path,
            containingFile = "Test.java",
            line = 1,
            annotations = emptyList()
        )
    }
}