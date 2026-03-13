package org.ripple.endpoint.detector.grpc

import org.ripple.endpoint.detector.api.DetectorConfig
import org.ripple.endpoint.detector.api.TrafficDetector
import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.TrafficEntry

class GrpcDetector : TrafficDetector {
    
    override val entryType = EntryType.GRPC
    override val name = "gRPC"
    override val description = "Detects gRPC service implementations"
    
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
        
        // Check for @GrpcService annotation
        val grpcServicePattern = Regex(
            """@GrpcService\s*(?:\([^)]*\))?\s*(?:public\s+)?class\s+(\w+)""",
            RegexOption.MULTILINE
        )
        
        val grpcMatch = grpcServicePattern.find(sourceCode)
        if (grpcMatch != null) {
            val className = grpcMatch.groupValues[1]
            
            // Extract service name from class name (e.g., UserServiceImpl -> UserService)
            val serviceName = className.removeSuffix("Impl").removeSuffix("Service")
            
            // Find all public methods that could be gRPC endpoints
            val methodPattern = Regex(
                """(?:@Override\s+)?public\s+\w+(?:<[^>]+>)?\s+(\w+)\s*\(""",
                RegexOption.MULTILINE
            )
            
            val lines = sourceCode.lines()
            methodPattern.findAll(sourceCode).forEach { match ->
                val methodName = match.groupValues[1]
                
                // Filter out common non-gRPC methods
                if (methodName !in listOf("toString", "hashCode", "equals", "clone", "finalize", 
                    "getClass", "notify", "notifyAll", "wait")) {
                    
                    val lineNumber = sourceCode.substring(0, match.range.first).lines().size
                    
                    entries.add(TrafficEntry(
                        type = EntryType.GRPC,
                        name = methodName,
                        path = "$serviceName/$methodName",
                        containingFile = filePath,
                        line = lineNumber,
                        annotations = DEFAULT_ANNOTATIONS,
                        metadata = mapOf(
                            "serviceName" to serviceName,
                            "className" to className
                        )
                    ))
                }
            }
        }
        
        return entries.distinctBy { it.id }
    }
    
    override fun isApplicable(filePath: String, config: DetectorConfig): Boolean {
        val packages = config.basePackages.ifEmpty { DEFAULT_PACKAGES }
        return packages.any { pkg -> 
            filePath.contains("/$pkg/") || filePath.contains("\\$pkg\\")
        } && (filePath.endsWith(".java") || filePath.endsWith(".kt"))
    }
    
    companion object {
        val DEFAULT_ANNOTATIONS = listOf(
            "io.grpc.stub.annotations.GrpcService"
        )
        val DEFAULT_PACKAGES = listOf("grpc", "rpc", "service")
    }
}