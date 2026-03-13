# IDEA 流量入口检测插件

基于 Git 变更自动检测受影响的流量入口，支持多框架、可扩展配置。

## 功能特性

- ✅ **多框架支持**: Spring MVC、gRPC、RocketMQ/Kafka/RabbitMQ、Scheduled
- ✅ **Git 集成**: 工作区变更、暂存区变更、分支对比
- ✅ **Commit 前置检查**: 自动检测并提供风险提示
- ✅ **调用链追踪**: 完整调用图，支持 DOT/GraphML/JSON 导出
- ✅ **报告生成**: Markdown/JSON/HTML 格式
- ✅ **可扩展配置**: 支持自定义注解、包路径、敏感规则

## 项目结构

```
idea-endpoint-plugin/
├── detector-api/      # SPI 接口定义
├── core/              # 核心引擎
│   ├── model/        # 领域模型
│   ├── engine/       # 检测引擎
│   ├── git/          # Git 变更解析
│   ├── config/       # 配置管理
│   ├── tracer/       # 调用链追踪
│   └── graph/        # 图导出
├── detectors/        # 检测器实现
│   ├── spring-mvc/   # HTTP 端点
│   ├── grpc/         # gRPC 服务
│   ├── mq/           # 消息队列
│   └── scheduled/    # 定时任务
├── ui/               # UI 组件
├── report/           # 报告生成
├── plugin/           # 插件入口
└── tests/            # 测试
```

## 构建

```bash
# 安装 Gradle Wrapper
gradle wrapper --gradle-version 8.5

# 构建
./gradlew build

# 运行测试
./gradlew test

# 运行插件（在 IDEA 中）
./gradlew runIde
```

## 使用方式

### 1. 检测变更

- **菜单**: Git → 流量入口检测 → 检测变更
- **快捷键**: `Ctrl+Alt+D`

### 2. 分支对比

- **菜单**: Git → 流量入口检测 → 分支对比
- 选择源分支和目标分支进行对比

### 3. 生成报告

- **菜单**: Git → 流量入口检测 → 生成报告
- 报告保存在 `flow-detector-report/` 目录

### 4. Commit 前置检查

提交代码时自动触发检测，高风险变更会弹出确认对话框。

## 配置

### Settings → Tools → 流量入口检测

| 配置项 | 说明 |
|--------|------|
| 启用 Commit 前检测 | 是否在提交前自动检测 |
| 高风险变更拦截 Commit | 高风险时是否阻止提交 |
| 报告保存路径 | 报告文件保存目录 |
| 最大追踪深度 | 调用链追踪最大深度 |

### 团队共享配置

在项目根目录创建 `.endpoint-detector.yaml`:

```yaml
version: "1.0"

sensitivePatterns:
  - pattern: "**/payment/**"
    reason: "支付相关接口"
    risk: HIGH
  - pattern: "**/auth/**"
    reason: "鉴权相关接口"
    risk: HIGH

ignoreRules:
  - pattern: "**/internal/**"
    reason: "内部API"

riskThresholds:
  blockCommitOn: [HIGH]
  notifyOn: [MEDIUM, LOW]
```

## 扩展检测器

实现 `TrafficDetector` 接口：

```kotlin
class MyCustomDetector : TrafficDetector {
    override val entryType = EntryType.CUSTOM
    override val name = "MyCustom"
    override val description = "自定义检测器"
    
    override fun detect(
        sourceCode: String,
        filePath: String,
        config: DetectorConfig
    ): List<TrafficEntry> {
        // 检测逻辑
    }
    
    override fun isApplicable(
        filePath: String,
        config: DetectorConfig
    ): Boolean {
        // 判断文件是否适用
    }
}
```

在 `plugin.xml` 中注册：

```xml
<extensions defaultExtensionNs="com.xxx.idea-endpoint-detector">
    <trafficDetector implementation="com.example.MyCustomDetector"/>
</extensions>
```

## 技术栈

- Kotlin 1.9.22
- IntelliJ Platform Plugin SDK
- Gradle 8.5 + Kotlin DSL
- JUnit 5 + MockK

## License

MIT