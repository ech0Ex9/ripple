# IDEA 流量入口检测插件设计文档

> 生成时间: 2026-03-13
> 状态: 已确认

## 一、项目概述

### 1.1 目标

基于 Git 变更（Changelist/分支对比），自动检测受影响的流量入口，支持多框架、可扩展配置，适配开发上线全流程，降低变更漏检风险。

### 1.2 技术栈

| 项目 | 选择 |
|------|------|
| 语言 | Kotlin 1.9.22 |
| 运行时 | Java 17 (IDEA 2025.3 内置) |
| 构建 | Gradle 8.x + Kotlin DSL |
| 目标 IDE | IntelliJ IDEA 2025.3 (since-build 253) |
| UI | Kotlin UI DSL |
| 测试 | JUnit 5 + MockK |

### 1.3 核心约束

1. 适配 IDEA 2025.3 版本
2. 不写死检测注解，支持自定义
3. 避免弹窗展示，支持 Git Commit 前置触发
4. 支持分支对比及上线前报告生成
5. 支持调用链路追踪与 Graph 格式导出

---

## 二、架构设计

### 2.1 模块划分

```
idea-endpoint-plugin/
├── detector-api/      # 检测器 SPI 接口
├── core/              # 核心引擎（模型、检测、追踪、配置）
├── detectors/         # 检测器实现
│   ├── spring-mvc/
│   ├── grpc/
│   ├── rocketmq/
│   └── scheduled/
├── ui/                # UI 组件
├── report/            # 报告生成
├── plugin/            # 插件入口
└── tests/             # 集成测试
```

### 2.2 模块职责

| 模块 | 职责 | 依赖 |
|------|------|------|
| detector-api | SPI 接口定义 | 无 |
| core | 检测引擎、模型、配置 | detector-api |
| detectors/* | 各框架检测器实现 | detector-api, core |
| ui | 配置面板、结果展示 | core |
| report | 报告生成 | core |
| plugin | 插件入口、集成 | 所有模块 |

### 2.3 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        plugin                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   action    │  │   commit    │  │      service        │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
└─────────┼────────────────┼────────────────────┼─────────────┘
          │                │                    │
          ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│                          ui                                  │
│  ┌──────────┐ ┌────────────┐ ┌───────────┐ ┌─────────────┐  │
│  │ settings │ │ toolwindow │ │   dialog  │ │ notification│  │
│  └──────────┘ └────────────┘ └───────────┘ └─────────────┘  │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                         core                                 │
│  ┌───────┐ ┌────────┐ ┌────────┐ ┌─────┐ ┌────────┐ ┌────┐  │
│  │ model │ │ engine │ │ tracer │ │ git │ │ config │ │graph│  │
│  └───┬───┘ └────────┘ └────────┘ └─────┘ └────────┘ └────┘  │
└──────┼──────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                      detector-api                            │
│              TrafficDetector (SPI Interface)                 │
└─────────────────────────────────────────────────────────────┘
       ▲
       │ implements
┌──────┴──────────────────────────────────────────────────────┐
│                       detectors                              │
│  ┌───────────┐ ┌──────────┐ ┌───────────┐ ┌───────────────┐ │
│  │ spring-mvc│ │   grpc   │ │  rocketmq │ │   scheduled   │ │
│  └───────────┘ └──────────┘ └───────────┘ └───────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、核心模型

### 3.1 领域模型

```kotlin
// 流量入口定义
data class TrafficEntry(
    val type: EntryType,           // HTTP, GRPC, MQ, SCHEDULED, CUSTOM
    val name: String,              // 入口名称
    val path: String?,             // 路径
    val containingFile: String,    // 所在文件
    val line: Int,                 // 行号
    val annotations: List<String>, // 相关注解
    val metadata: Map<String, Any> // 额外元信息
)

// 影响级别
enum class ImpactLevel { HIGH, MEDIUM, LOW }

// 检测结果
data class DetectionResult(
    val entry: TrafficEntry,
    val impactLevel: ImpactLevel,
    val changedFiles: List<String>,
    val reason: String
)

// 变更文件
data class ChangedFile(
    val path: String,
    val changeType: ChangeType,  // ADD, MODIFY, DELETE
    val content: String?
)
```

### 3.2 调用图模型

```kotlin
// 调用图节点
data class CallGraphNode(
    val id: String,
    val type: NodeType,           // TRAFFIC_ENTRY, CLASS, METHOD, FIELD, EXTERNAL
    val name: String,
    val qualifiedName: String,
    val filePath: String,
    val line: Int?,
    val metadata: Map<String, Any>
)

// 调用边
data class CallEdge(
    val from: String,
    val to: String,
    val relation: CallRelation,   // CALLS, IMPLEMENTS, REFERENCES, ANNOTATED_BY, HANDLES, INVOKES
    val metadata: Map<String, Any>
)

// 完整调用图
data class CallGraph(
    val nodes: Map<String, CallGraphNode>,
    val edges: List<CallEdge>,
    val entryPoints: List<String>,
    val changedNodes: Set<String>,
    val metadata: GraphMetadata
)
```

---

## 四、检测器 SPI

### 4.1 接口定义

```kotlin
interface TrafficDetector {
    val entryType: EntryType
    val name: String
    val description: String
    
    fun isApplicable(file: PsiFile, config: DetectorConfig): Boolean
    fun detectEntries(file: PsiFile, config: DetectorConfig): List<TrafficEntry>
    fun isEntryPoint(method: PsiMethod, config: DetectorConfig): Boolean
}
```

### 4.2 扩展点配置

```xml
<extensionPoints>
    <extensionPoint name="trafficDetector" 
                    interface="com.xxx.endpoint.detector.api.TrafficDetector"
                    dynamic="true"/>
</extensionPoints>

<extensions defaultExtensionNs="com.xxx.idea-endpoint-detector">
    <trafficDetector implementation="...SpringMvcDetector"/>
    <trafficDetector implementation="...GrpcDetector"/>
    <trafficDetector implementation="...RocketMqDetector"/>
    <trafficDetector implementation="...ScheduledDetector"/>
</extensions>
```

---

## 五、影响级别判定

### 5.1 判定规则

| 变更类型 | 风险级别 | 理由 |
|---------|---------|------|
| 删除入口方法 | HIGH | 可能导致调用方 404 |
| 修改方法签名（参数/返回值变化） | HIGH | 可能破坏调用方兼容性 |
| 修改敏感入口（支付、鉴权、核心业务） | HIGH | 业务风险高 |
| 新增入口 | MEDIUM | 需要关注，但风险可控 |
| 修改入口实现逻辑 | MEDIUM | 业务变更，需测试覆盖 |
| 修改入口依赖（Service/Util） | MEDIUM | 间接影响 |
| 修改注释/格式 | LOW | 无实际影响 |

### 5.2 敏感入口配置

```yaml
sensitivePatterns:
  - pattern: "**/payment/**"
    reason: "支付相关接口"
    risk: HIGH
  - pattern: "**/auth/**"
    reason: "鉴权相关接口"
    risk: HIGH
  - pattern: "**/admin/**"
    reason: "管理后台接口"
    risk: HIGH
```

---

## 六、配置层级

### 6.1 三层配置

| 层级 | 存储位置 | 用途 |
|------|----------|------|
| GLOBAL | ~/.config/idea-endpoint-detector/config.yaml | 用户默认配置 |
| PROJECT | .idea/endpoint-detector/config.yaml | 项目本地配置（不提交） |
| SHARED | .endpoint-detector.yaml | 团队共享配置（提交 Git） |

### 6.2 合并策略

项目 > 共享 > 全局

### 6.3 共享配置示例

```yaml
version: "1.0"

sensitivePatterns:
  - pattern: "**/payment/**"
    entryTypes: [HTTP, GRPC]
    reason: "支付相关接口，变更需谨慎"
    risk: HIGH

ignoreRules:
  - pattern: "**/internal/**"
    reason: "内部API，不对外"

detectorPresets:
  HTTP:
    annotations:
      - org.springframework.web.bind.annotation.RequestMapping
    basePackages:
      - com.example.controller

riskThresholds:
  blockCommitOn: [HIGH]
  notifyOn: [MEDIUM, LOW]
```

---

## 七、调用链路追踪

### 7.1 追踪器接口

```kotlin
interface CallGraphTracer {
    fun traceFromEntry(entry: TrafficEntry, psiFile: PsiFile, maxDepth: Int = 20): CallGraph
    fun traceFromEntries(entries: List<TrafficEntry>, maxDepth: Int = 20): CallGraph
}

interface ReverseTracer {
    fun traceToEntries(changedFiles: List<ChangedFile>, allEntries: List<TrafficEntry>): List<ImpactPath>
}
```

### 7.2 输出格式

- **DOT** - Graphviz 格式，可视化
- **GraphML** - 结构化图格式
- **JSON** - 大模型分析用

---

## 八、UI 设计

### 8.1 配置面板

```
┌─ 流量入口检测配置 ──────────────────────────────────────────┐
│ 配置范围: [全局 ▼] [项目] [团队共享]                       │
├─────────────────────────────────────────────────────────────┤
│ ┌─ 框架检测配置 ────────────────────────────────────────────┐│
│ │ ☑ Spring MVC    注解: [RequestMapping________] [编辑]    ││
│ │ ☑ gRPC          注解: [GrpcService__________] [编辑]     ││
│ │ ☑ RocketMQ      注解: [RocketMQMessageListener] [编辑]   ││
│ │ ☑ Scheduled     注解: [Scheduled___________] [编辑]      ││
│ └─────────────────────────────────────────────────────────┘│
│ ┌─ 通用配置 ────────────────────────────────────────────────┐│
│ │ ☑ 启用 Commit 前检测                                     ││
│ │ ☑ 高风险变更拦截 Commit                                  ││
│ └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 8.2 结果面板

```
┌─ 流量入口检测结果 ─────────────────────────────────────┐
│ 🔍 [搜索...]                    [刷新] [导出] [生成报告]│
├─────────────────────────────────────────────────────────┤
│ ▼ 高风险 (1)                                           │
│   ├─ 🌐 HTTP: POST /api/orders                         │
│ │    └─ OrderController.java:45 [跳转]                │
│ ▼ 中风险 (2)                                           │
│   ├─ 📨 MQ: OrderCreatedConsumer                      │
│   └─ ⏰ Scheduled: syncInventoryTask                  │
├─────────────────────────────────────────────────────────┤
│ 📊 统计: 共 3 个受影响入口 | 高 1 中 2 低 0             │
└─────────────────────────────────────────────────────────┘
```

---

## 九、报告生成

### 9.1 支持格式

| 格式 | 用途 |
|------|------|
| Markdown | 上线评审、团队共享 |
| JSON | 大模型分析 |
| Excel | 统计分析（可选） |

### 9.2 Markdown 报告结构

```markdown
# 流量入口检测报告

## 📁 变更概览
## 🎯 受影响的流量入口
### 🔴 高风险
### 🟡 中风险
### 🟢 低风险
## 📊 统计信息
## 🔗 调用图数据
```

---

## 十、开发阶段规划

```
Phase 1: 骨架搭建（Week 1-2）
├── 项目结构初始化
├── Gradle 配置
├── detector-api 模块
├── core/model 子模块
└── 基础测试框架

Phase 2: 核心引擎（Week 3-4）
├── core/engine 子模块
├── core/git 子模块
├── core/config 子模块
└── 单元测试

Phase 3: 调用链追踪（Week 5-6）
├── core/tracer 子模块
├── core/graph 子模块
└── DOT/GraphML 导出

Phase 4: 检测器实现（Week 7-8）
├── detectors/spring-mvc
├── detectors/grpc
├── detectors/rocketmq
└── detectors/scheduled

Phase 5: UI 集成（Week 9-10）
├── ui/settings 子模块
├── ui/toolwindow 子模块
├── ui/notification 子模块
└── plugin 模块集成

Phase 6: 报告与测试（Week 11-12）
├── report 模块
├── 集成测试
├── 文档编写
└── 发布准备
```

---

## 十一、验收标准

1. **规则配置**：可正常打开可视化面板，勾选框架、编辑规则、保存生效
2. **Git 变更解析**：可正常解析工作区变更、暂存区变更、分支对比
3. **流量入口检测**：可正确检测 Spring MVC、gRPC、MQ、定时任务
4. **调用链追踪**：可生成完整调用图，导出 DOT/GraphML/JSON
5. **触发机制**：Git Commit 前可自动触发检测，手动触发正常
6. **报告生成**：可生成 Markdown/JSON 报告
7. **兼容性**：在 IDEA 2025.3 版本中可正常运行

---

## 附录：原始 PRD

详见: `~/Downloads/IDEA 流量入口检测插件 PRD 文档.md`