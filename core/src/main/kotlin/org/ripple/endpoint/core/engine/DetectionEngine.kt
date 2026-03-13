package org.ripple.endpoint.core.engine

import org.ripple.endpoint.core.model.DetectionResult
import org.ripple.endpoint.core.model.DetectionSummary
import org.ripple.endpoint.core.model.CallGraph
import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.model.ChangedFile

data class DetectionRequest(
    val projectPath: String,
    val changedFiles: List<ChangedFile>,
    val config: Map<String, DetectorConfig>
)

data class DetectionResponse(
    val results: List<DetectionResult>,
    val callGraph: CallGraph?,
    val summary: DetectionSummary,
    val errors: List<DetectionError>
)

data class DetectionError(
    val file: String,
    val message: String,
    val exception: Exception? = null
)