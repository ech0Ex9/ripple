# AGENTS.md

Guidelines for AI coding agents working in this repository.

## Project Overview

IntelliJ IDEA plugin for traffic entry detection based on Git changes. Detects affected HTTP, gRPC, MQ, and Scheduled endpoints.

## Build Commands

```bash
# Build entire project
./gradlew build

# Build plugin ZIP (for installation)
./gradlew buildPlugin

# Output: plugin/build/distributions/plugin-1.0.0-SNAPSHOT.zip

# Run tests
./gradlew test

# Run single test class
./gradlew :core:test --tests "org.ripple.endpoint.core.engine.DefaultDetectionEngineTest"

# Run single test method
./gradlew :core:test --tests "org.ripple.endpoint.core.engine.DefaultDetectionEngineTest.should detect entries from changed files"

# Compile only (faster iteration)
./gradlew compileKotlin

# Clean build
./gradlew clean build
```

## Project Structure

```
ripple/
├── detector-api/           # SPI interfaces (TrafficDetector, models)
├── core/                   # Detection engine, Git resolver, graph exporters
├── detectors/
│   ├── spring-mvc/         # HTTP endpoint detector
│   ├── grpc/               # gRPC service detector
│   ├── mq/                 # Message queue detector
│   └── scheduled/          # Scheduled task detector
├── ui/                     # Tool window, dialogs, notifications
├── report/                 # Markdown/JSON report generators
├── plugin/                 # Plugin entry point, actions, services
└── tests/                  # Integration tests
```

## Key Files

| Purpose | File |
|---------|------|
| Plugin config | `plugin/src/main/resources/META-INF/plugin.xml` |
| Dependency versions | `buildSrc/src/main/kotlin/Dependencies.kt` |
| Root build | `build.gradle.kts` |
| Settings | `settings.gradle.kts`, `gradle.properties` |

## Code Style

### Kotlin Conventions

- **Package naming**: `org.ripple.endpoint.<module>`
- **Class naming**: PascalCase for classes, camelCase for functions/properties
- **File naming**: Same as the primary class (e.g., `DefaultDetectionEngine.kt`)
- **Indentation**: 4 spaces, no tabs
- **Imports**: Organized by package depth, no wildcard imports

### Package Structure

```
org.ripple.endpoint
├── core
│   ├── engine/         # DetectionEngine, DetectionRequest/Response
│   ├── git/            # GitChangeResolver, BranchDiffResult
│   ├── graph/          # GraphExporter implementations
│   ├── model/          # CallGraph, DetectionResult, DetectionSummary
│   └── tracer/         # CallGraphTracer
├── detector.api        # TrafficDetector interface, model classes
└── ui
    ├── dialog/         # DialogWrapper subclasses
    ├── notification/   # EndpointNotifier
    └── toolwindow/     # ResultTreePanel, tool window factory
```

### Imports Order

```kotlin
// 1. Java standard library
import java.nio.file.Files
import java.nio.file.Path

// 2. Kotlin/IntelliJ Platform
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Test

// 3. Project internal
import org.ripple.endpoint.core.model.DetectionResult
import org.ripple.endpoint.detector.api.TrafficDetector
```

### Naming Patterns

| Type | Pattern | Example |
|------|---------|---------|
| Interface | Noun | `TrafficDetector`, `DetectionEngine` |
| Implementation | `Default` prefix | `DefaultDetectionEngine`, `DefaultGitChangeResolver` |
| Data class | Noun | `DetectionResult`, `TrafficEntry`, `CallGraph` |
| Test class | `<Class>Test` | `DefaultDetectionEngineTest` |
| Test method | backtick description | `` `should return empty results when no changes` `` |

### Error Handling

```kotlin
// Use nullable return types for optional results
fun readFile(path: String): String? {
    return try {
        Files.readString(Path.of(path))
    } catch (e: Exception) {
        null
    }
}

// Collect errors in a list rather than throwing
val errors = mutableListOf<DetectionError>()
if (sourceCode == null) {
    errors.add(DetectionError(path, "Cannot read file"))
}
```

### Data Classes

```kotlin
// Prefer data classes for models
data class DetectionResult(
    val entry: TrafficEntry,
    val impactLevel: ImpactLevel,
    val impactReason: String,
    val changedFiles: List<String>,
    val callPath: List<CallGraphNode> = emptyList()  // Default values for optional fields
)
```

### Extension Patterns

When adding a new detector:

1. Implement `TrafficDetector` interface
2. Place in `detectors/<name>/` module
3. Register in `plugin.xml` if needed as extension
4. Add unit tests in `src/test/kotlin/`

```kotlin
class MyDetector : TrafficDetector {
    override val entryType = EntryType.CUSTOM
    override val name = "MyDetector"
    override val description = "Detects my custom endpoints"
    override val defaultConfig = DetectorConfig()
    
    override fun detect(sourceCode: String, filePath: String, config: DetectorConfig): List<TrafficEntry> {
        // Detection logic
    }
    
    override fun isApplicable(filePath: String, config: DetectorConfig): Boolean {
        return filePath.endsWith(".java")
    }
}
```

## Testing

- **Framework**: JUnit 5 + MockK
- **Location**: `src/test/kotlin/` mirrors `src/main/kotlin/`
- **Test naming**: Backtick strings describing behavior

```kotlin
@Test
fun `should detect entries from changed files`() {
    // Given
    val detector = MockDetector()
    val engine = DefaultDetectionEngine(listOf(detector))
    
    // When
    val response = engine.detect(request)
    
    // Then
    assertTrue(response.summary.totalEntries >= 0)
}
```

## IntelliJ Platform Notes

- **Target version**: IDEA 2025.3 (build 253.*)
- **JVM target**: Java 17
- **Kotlin version**: 2.2.0 (root) / 1.9.22 (properties)

### Common IntelliJ APIs

```kotlin
// Progress background task
ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Task name", true) {
    override fun run(indicator: ProgressIndicator) {
        indicator.text = "Processing..."
        indicator.fraction = 0.5
        // Work here
    }
})

// Notification
NotificationGroupManager.getInstance()
    .getNotificationGroup("Endpoint Detector")
    .createNotification(message, NotificationType.INFORMATION)
    .notify(project)

// Run on EDT
SwingUtilities.invokeLater {
    // UI updates
}
```

## Git Commit Messages

```
feat(module): brief description
fix(module): brief description
refactor(module): brief description
docs: brief description
```

## Module Dependencies

```
plugin → ui → core → detector-api
plugin → report
plugin → detectors/* → detector-api
```

## Quick Reference

| Task | Command |
|------|---------|
| Build plugin | `./gradlew buildPlugin` |
| Run tests | `./gradlew test` |
| Single test | `./gradlew :core:test --tests "FullyQualifiedTestName"` |
| Compile | `./gradlew compileKotlin` |
| Clean | `./gradlew clean` |