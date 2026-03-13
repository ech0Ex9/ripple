package org.ripple.endpoint.plugin.service

import com.intellij.openapi.project.Project
import org.ripple.endpoint.core.engine.DefaultDetectionEngine
import org.ripple.endpoint.core.engine.DetectionEngine
import org.ripple.endpoint.core.git.DefaultGitChangeResolver
import org.ripple.endpoint.core.git.GitChangeResolver
import org.ripple.endpoint.detector.api.TrafficDetector
import org.ripple.endpoint.detector.grpc.GrpcDetector
import org.ripple.endpoint.detector.mq.MqDetector
import org.ripple.endpoint.detector.scheduled.ScheduledDetector
import org.ripple.endpoint.detector.spring.SpringMvcDetector

class DetectionService(private val project: Project) {
    
    private val detectors: List<TrafficDetector> = listOf(
        SpringMvcDetector(),
        GrpcDetector(),
        MqDetector(),
        ScheduledDetector()
    )
    
    private val engine: DetectionEngine = DefaultDetectionEngine(detectors)
    private val gitResolver: GitChangeResolver = DefaultGitChangeResolver()
    
    fun getDetectors(): List<TrafficDetector> = detectors
    
    fun getEngine(): DetectionEngine = engine
    
    fun getGitResolver(): GitChangeResolver = gitResolver
    
    fun getProjectPath(): String? = project.basePath
    
    companion object {
        fun getInstance(project: Project): DetectionService {
            return project.getService(DetectionService::class.java)
        }
    }
}