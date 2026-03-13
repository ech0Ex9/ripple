package org.ripple.endpoint.core.tracer

import org.ripple.endpoint.core.model.CallGraph
import org.ripple.endpoint.core.model.ImpactPath
import org.ripple.endpoint.detector.api.model.ChangedFile
import org.ripple.endpoint.detector.api.model.TrafficEntry

interface CallGraphTracer {
    
    fun traceFromEntry(
        entry: TrafficEntry,
        sourceCode: String,
        filePath: String,
        maxDepth: Int = 20
    ): CallGraph
    
    fun traceFromEntries(
        entries: List<TrafficEntry>,
        sources: Map<String, String>,
        maxDepth: Int = 20
    ): CallGraph
}

interface ReverseTracer {
    
    fun traceToEntries(
        changedFiles: List<ChangedFile>,
        allEntries: List<TrafficEntry>,
        callGraph: CallGraph
    ): List<ImpactPath>
}