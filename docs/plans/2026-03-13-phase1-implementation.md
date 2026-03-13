# IDEA 流量入口检测插件 - Phase 1 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 搭建项目骨架，完成 Gradle 配置、detector-api 模块和 core/model 子模块

**Architecture:** 多模块 Gradle 项目，使用 Kotlin DSL 构建，SPI 扩展点设计

**Tech Stack:** Kotlin 1.9.22, Gradle 8.x, IntelliJ Platform Plugin SDK, JUnit 5

---

## Task 1: 项目目录结构初始化

**Files:**
- Create: 项目根目录结构

**Step 1: 创建根目录**

```bash
cd ~/development/idea-endpoint-plugin
```

**Step 2: 创建基础目录结构**

```bash
mkdir -p detector-api/src/main/kotlin/com/xxx/endpoint/detector/api
mkdir -p core/src/main/kotlin/com/xxx/endpoint/core/{model,engine,tracer,graph,git,config}
mkdir -p core/src/test/kotlin/com/xxx/endpoint/core
mkdir -p detectors/spring-mvc/src/main/kotlin/com/xxx/endpoint/detector/spring
mkdir -p detectors/grpc/src/main/kotlin/com/xxx/endpoint/detector/grpc
mkdir -p detectors/rocketmq/src/main/kotlin/com/xxx/endpoint/detector/rocketmq
mkdir -p detectors/scheduled/src/main/kotlin/com/xxx/endpoint/detector/scheduled
mkdir -p ui/src/main/kotlin/com/xxx/endpoint/ui/{settings,toolwindow,notification,dialog,components}
mkdir -p report/src/main/kotlin/com/xxx/endpoint/report/{model,generator,exporter}
mkdir -p plugin/src/main/kotlin/com/xxx/endpoint/plugin/{action,commit,service}
mkdir -p plugin/src/main/resources/META-INF
mkdir -p plugin/src/main/resources/icons
mkdir -p tests/src/test/kotlin/com/xxx/endpoint/{detector,engine,report}
mkdir -p gradle/wrapper
mkdir -p buildSrc/src/main/kotlin
```

**Step 3: 验证目录结构**

```bash
find . -type d | head -30
```

Expected: 显示创建的目录结构

---

## Task 2: Gradle Wrapper 配置

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew` (从现有项目复制或生成)

**Step 1: 创建 gradle-wrapper.properties**

```bash
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
```

**Step 2: 生成 Gradle Wrapper（如果系统有 Gradle）**

```bash
gradle wrapper --gradle-version 8.5
```

或手动下载 wrapper 文件。

---

## Task 3: Gradle 配置文件

**Files:**
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `build.gradle.kts`
- Create: `buildSrc/build.gradle.kts`
- Create: `buildSrc/src/main/kotlin/Dependencies.kt`

**Step 1: 创建 settings.gradle.kts**

```kotlin
rootProject.name = "idea-endpoint-plugin"

include(
    "detector-api",
    "core",
    "detectors:spring-mvc",
    "detectors:grpc",
    "detectors:rocketmq",
    "detectors:scheduled",
    "ui",
    "report",
    "plugin",
    "tests"
)
```

**Step 2: 创建 gradle.properties**

```properties
# Gradle
org.gradle.jvmargs=-Xmx2g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true

# Kotlin
kotlin.code.style=official

# IntelliJ Platform
ideaVersion=2025.3
sinceBuild=253
untilBuild=253.*
pluginGroup=com.xxx.endpoint
pluginName=流量入口检测
pluginVersion=1.0.0

# Dependencies
kotlinVersion=1.9.22
intellijPluginVersion=1.17.0
junitVersion=5.10.1
mockkVersion=1.13.8
kotlinxSerializationVersion=1.6.2
```

**Step 3: 创建根 build.gradle.kts**

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("org.jetbrains.intellij") version "1.17.0" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
}

group = "com.xxx"
version = "1.0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

**Step 4: 创建 buildSrc/build.gradle.kts**

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}
```

**Step 5: 创建 buildSrc/src/main/kotlin/Dependencies.kt**

```kotlin
object Versions {
    const val KOTLIN = "1.9.22"
    const val INTELLIJ_PLUGIN = "1.17.0"
    const val JUNIT = "5.10.1"
    const val MOCKK = "1.13.8"
    const val KOTLINX_SERIALIZATION = "1.6.2"
    const val APACHE_POI = "5.2.5"
}

object Libs {
    object Kotlin {
        const val STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
        const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    }
    
    object Kotlinx {
        const val SERIALIZATION_JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}"
    }
    
    object Test {
        const val JUNIT_API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}"
        const val JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}"
        const val MOCKK = "io.mockk:mockk:${Versions.MOCKK}"
    }
    
    object Apache {
        const val POI = "org.apache.poi:poi-ooxml:${Versions.APACHE_POI}"
    }
}
```

**Step 6: 验证 Gradle 配置**

```bash
./gradlew --version
```

Expected: 显示 Gradle 8.5 版本信息

---

## Task 4: detector-api 模块

**Files:**
- Create: `detector-api/build.gradle.kts`
- Create: `detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/TrafficDetector.kt`
- Create: `detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/DetectionContext.kt`
- Create: `detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/DetectorConfig.kt`

**Step 1: 创建 detector-api/build.gradle.kts**

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Libs.Kotlin.STDLIB)
    implementation(Libs.Kotlin.COROUTINES)
    
    testImplementation(Libs.Test.JUNIT_API)
    testImplementation(Libs.Test.JUNIT_ENGINE)
    testImplementation(Libs.Test.MOCKK)
}
```

**Step 2: 创建核心枚举和模型**

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/model/EntryType.kt
package com.xxx.endpoint.detector.api.model

enum class EntryType {
    HTTP,
    GRPC,
    MQ,
    SCHEDULED,
    CUSTOM
}
```

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/model/TrafficEntry.kt
package com.xxx.endpoint.detector.api.model

data class TrafficEntry(
    val type: EntryType,
    val name: String,
    val path: String?,
    val containingFile: String,
    val line: Int,
    val annotations: List<String>,
    val metadata: Map<String, Any> = emptyMap()
) {
    val id: String
        get() = "$containingFile:$name"
}
```

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/model/ImpactLevel.kt
package com.xxx.endpoint.detector.api.model

enum class ImpactLevel {
    HIGH,
    MEDIUM,
    LOW
}
```

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/model/ChangeType.kt
package com.xxx.endpoint.detector.api.model

enum class ChangeType {
    ADD,
    MODIFY,
    DELETE
}
```

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/model/ChangedFile.kt
package com.xxx.endpoint.detector.api.model

data class ChangedFile(
    val path: String,
    val changeType: ChangeType,
    val linesAdded: Int = 0,
    val linesDeleted: Int = 0,
    val content: String? = null
)
```

**Step 3: 创建 DetectorConfig**

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/DetectorConfig.kt
package com.xxx.endpoint.detector.api

import com.xxx.endpoint.detector.api.model.EntryType

data class DetectorConfig(
    val enabled: Boolean = true,
    val annotations: List<String> = emptyList(),
    val basePackages: List<String> = emptyList(),
    val customRules: List<CustomRule> = emptyList()
)

data class CustomRule(
    val name: String,
    val annotation: String,
    val basePackage: String,
    val entryType: EntryType = EntryType.CUSTOM
)
```

**Step 4: 创建 DetectionContext**

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/DetectionContext.kt
package com.xxx.endpoint.detector.api

import com.xxx.endpoint.detector.api.model.ChangedFile

data class DetectionContext(
    val projectPath: String,
    val changedFiles: List<ChangedFile>,
    val config: Map<String, DetectorConfig>
)
```

**Step 5: 创建 TrafficDetector 接口**

```kotlin
// detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/TrafficDetector.kt
package com.xxx.endpoint.detector.api

import com.xxx.endpoint.detector.api.model.EntryType
import com.xxx.endpoint.detector.api.model.TrafficEntry

/**
 * 流量入口检测器 SPI 接口
 * 所有框架检测器需实现此接口
 */
interface TrafficDetector {
    
    /**
     * 检测器支持的入口类型
     */
    val entryType: EntryType
    
    /**
     * 检测器名称
     */
    val name: String
    
    /**
     * 检测器描述
     */
    val description: String
    
    /**
     * 默认配置
     */
    val defaultConfig: DetectorConfig
    
    /**
     * 检测指定内容中的流量入口
     * @param sourceCode 源代码内容
     * @param filePath 文件路径
     * @param config 检测器配置
     * @return 检测到的流量入口列表
     */
    fun detect(
        sourceCode: String,
        filePath: String,
        config: DetectorConfig
    ): List<TrafficEntry>
    
    /**
     * 判断文件是否适用此检测器
     * @param filePath 文件路径
     * @param config 检测器配置
     * @return 是否适用
     */
    fun isApplicable(filePath: String, config: DetectorConfig): Boolean
}
```

**Step 6: 编译验证**

```bash
./gradlew :detector-api:build
```

Expected: BUILD SUCCESSFUL

---

## Task 5: core/model 子模块

**Files:**
- Create: `core/build.gradle.kts`
- Create: `core/src/main/kotlin/com/xxx/endpoint/core/model/CallGraph.kt`
- Create: `core/src/main/kotlin/com/xxx/endpoint/core/model/DetectionResult.kt`

**Step 1: 创建 core/build.gradle.kts**

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":detector-api"))
    
    implementation(Libs.Kotlin.STDLIB)
    implementation(Libs.Kotlin.COROUTINES)
    implementation(Libs.Kotlinx.SERIALIZATION_JSON)
    
    testImplementation(Libs.Test.JUNIT_API)
    testImplementation(Libs.Test.JUNIT_ENGINE)
    testImplementation(Libs.Test.MOCKK)
}
```

**Step 2: 创建调用图模型**

```kotlin
// core/src/main/kotlin/com/xxx/endpoint/core/model/CallGraph.kt
package com.xxx.endpoint.core.model

import kotlinx.serialization.Serializable

/**
 * 调用图节点
 */
@Serializable
data class CallGraphNode(
    val id: String,
    val type: NodeType,
    val name: String,
    val qualifiedName: String,
    val filePath: String,
    val line: Int? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 节点类型
 */
@Serializable
enum class NodeType {
    TRAFFIC_ENTRY,
    CLASS,
    METHOD,
    FIELD,
    EXTERNAL
}

/**
 * 调用边
 */
@Serializable
data class CallEdge(
    val from: String,
    val to: String,
    val relation: CallRelation,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 调用关系类型
 */
@Serializable
enum class CallRelation {
    CALLS,
    IMPLEMENTS,
    REFERENCES,
    ANNOTATED_BY,
    HANDLES,
    INVOKES
}

/**
 * 完整调用图
 */
@Serializable
data class CallGraph(
    val nodes: Map<String, CallGraphNode>,
    val edges: List<CallEdge>,
    val entryPoints: List<String>,
    val changedNodes: Set<String> = emptySet(),
    val metadata: GraphMetadata
)

/**
 * 图元数据
 */
@Serializable
data class GraphMetadata(
    val project: String,
    val generatedAt: String,
    val sourceBranch: String? = null,
    val targetBranch: String? = null
)
```

**Step 3: 创建检测结果模型**

```kotlin
// core/src/main/kotlin/com/xxx/endpoint/core/model/DetectionResult.kt
package com.xxx.endpoint.core.model

import com.xxx.endpoint.detector.api.model.ImpactLevel
import com.xxx.endpoint.detector.api.model.TrafficEntry

/**
 * 检测结果
 */
data class DetectionResult(
    val entry: TrafficEntry,
    val impactLevel: ImpactLevel,
    val impactReason: String,
    val changedFiles: List<String>,
    val callPath: List<CallGraphNode> = emptyList()
)

/**
 * 影响路径
 */
data class ImpactPath(
    val entry: TrafficEntry,
    val path: List<CallGraphNode>,
    val impactLevel: ImpactLevel
)

/**
 * 检测摘要
 */
data class DetectionSummary(
    val totalEntries: Int,
    val affectedEntries: Int,
    val byType: Map<String, TypeStats>,
    val byImpact: Map<ImpactLevel, Int>
)

/**
 * 类型统计
 */
data class TypeStats(
    val total: Int,
    val affected: Int,
    val highRisk: Int
)
```

**Step 4: 创建配置模型**

```kotlin
// core/src/main/kotlin/com/xxx/endpoint/core/model/Config.kt
package com.xxx.endpoint.core.model

import com.xxx.endpoint.detector.api.model.EntryType
import com.xxx.endpoint.detector.api.model.ImpactLevel
import com.xxx.endpoint.detector.api.DetectorConfig

/**
 * 全局配置
 */
data class GlobalConfig(
    val detectors: Map<EntryType, DetectorConfig>,
    val general: GeneralConfig
)

/**
 * 通用配置
 */
data class GeneralConfig(
    val maxTraceDepth: Int = 20,
    val reportOutputPath: String = "flow-detector-report",
    val commitCheckEnabled: Boolean = true,
    val highRiskBlockCommit: Boolean = true
)

/**
 * 敏感入口规则
 */
data class SensitivePattern(
    val pattern: String,
    val entryTypes: List<EntryType>? = null,
    val reason: String,
    val risk: ImpactLevel = ImpactLevel.HIGH
)

/**
 * 忽略规则
 */
data class IgnoreRule(
    val pattern: String,
    val reason: String
)

/**
 * 风险阈值配置
 */
data class RiskThresholds(
    val blockCommitOn: Set<ImpactLevel> = setOf(ImpactLevel.HIGH),
    val notifyOn: Set<ImpactLevel> = setOf(ImpactLevel.MEDIUM, ImpactLevel.LOW)
)

/**
 * 团队共享配置
 */
data class SharedConfig(
    val version: String = "1.0",
    val sensitivePatterns: List<SensitivePattern> = emptyList(),
    val ignoreRules: List<IgnoreRule> = emptyList(),
    val riskThresholds: RiskThresholds = RiskThresholds()
)
```

**Step 5: 编译验证**

```bash
./gradlew :core:build
```

Expected: BUILD SUCCESSFUL

---

## Task 6: 创建 .gitignore

**Files:**
- Create: `.gitignore`

**Step 1: 创建 .gitignore**

```gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
*.iws
*.ipr
out/

# Kotlin
*.class

# Plugin
plugin/build/
*/build/

# OS
.DS_Store
Thumbs.db

# Secrets
*.pem
*.key
.env

# Reports
flow-detector-report/
```

---

## Task 7: 初始化 Git 仓库

**Step 1: 初始化仓库**

```bash
git init
```

**Step 2: 添加所有文件**

```bash
git add .
```

**Step 3: 创建初始提交**

```bash
git commit -m "chore: initialize project structure

- Add multi-module Gradle configuration
- Add detector-api module with SPI interfaces
- Add core/model with domain models
- Add buildSrc with dependency management"
```

---

## Task 8: 验证整体构建

**Step 1: 执行完整构建**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

**Step 2: 检查项目结构**

```bash
find . -name "*.kt" | head -20
```

Expected: 显示所有创建的 Kotlin 文件

---

## Phase 1 完成标志

- [ ] 项目目录结构完整
- [ ] Gradle 配置正确，可执行 `./gradlew build`
- [ ] detector-api 模块可编译
- [ ] core/model 模块可编译
- [ ] Git 仓库已初始化并提交

---

## 后续 Phase

- **Phase 2**: 核心引擎实现（engine, git, config）
- **Phase 3**: 调用链追踪实现（tracer, graph）
- **Phase 4**: 检测器实现（spring-mvc, grpc, rocketmq, scheduled）
- **Phase 5**: UI 集成（settings, toolwindow, notification）
- **Phase 6**: 报告生成与测试