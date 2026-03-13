# 流量入口检测插件 - 功能规划 Roadmap

> 基于当前已实现功能，规划后续迭代方向

---

## 当前已实现 ✅

### 核心功能
- ✅ **增量检测** - 基于 Git diff 变更检测（非全量扫描）
- ✅ **多框架支持** - Spring MVC、gRPC、MQ、Scheduled
- ✅ **调用链追踪** - SimpleCallGraphTracer 实现
- ✅ **图导出** - DOT/GraphML/JSON 格式
- ✅ **基础风险分级** - HIGH/MEDIUM/LOW（基于变更类型和敏感模式）
- ✅ **Commit 前置检查** - 提交前自动检测
- ✅ **报告生成** - Markdown/JSON 格式

---

## 待实现功能 📋

### Phase 2: 可视化增强 (优先级: 高)

#### 2.1 调用图谱可视化渲染
**状态**: 🔴 待实现  
**描述**: 将 Graph 数据渲染为可视化图表（Mermaid/SVG），而非仅导出原始格式  
**验收标准**:
- [ ] 支持 Mermaid 流程图渲染
- [ ] 支持 SVG 矢量图导出
- [ ] 在工具窗口中展示交互式图谱
- [ ] 节点可点击跳转
- [ ] 支持缩放、拖拽、聚焦

**技术方案**:
```
方案 A: 内嵌 WebView + Mermaid.js
方案 B: JGraphT + Graphviz 渲染
方案 C: 调用外部浏览器打开
```

**相关文件**:
- `core/src/main/kotlin/com/xxx/endpoint/core/graph/GraphExporter.kt`
- `ui/src/main/kotlin/com/xxx/endpoint/ui/toolwindow/`

---

#### 2.2 调用链差异对比
**状态**: 🔴 待实现  
**描述**: 显示 "变更前 vs 变更后" 的调用链差异视图  
**验收标准**:
- [ ] 对比视图展示新增/删除/修改的调用节点
- [ ] 使用颜色标记差异（绿色=新增，红色=删除，黄色=修改）
- [ ] 支持按文件/方法筛选差异

---

### Phase 3: 风险分析增强 (优先级: 高)

#### 3.1 智能风险分级 V2
**状态**: 🟡 基础实现存在，需增强  
**当前实现**: `ImpactAnalyzer` 仅基于变更类型和敏感模式判断  
**增强方向**:

- [ ] **调用链深度因子** - 调用链越深，风险越高
  - 深度 > 5: 风险 +1 级
  - 深度 > 10: 风险 +2 级

- [ ] **下游影响面计算** - 统计受影响的下游服务数量
  - 影响 1-3 个服务: MEDIUM
  - 影响 >3 个服务: HIGH
  - 影响核心服务: CRITICAL (新增级别)

- [ ] **核心链路标记** - 支持配置核心业务流程
  ```yaml
  coreFlows:
    - name: "支付链路"
      patterns: ["**/payment/**", "**/order/pay/**"]
      risk: CRITICAL
    - name: "登录链路"
      patterns: ["**/auth/**", "**/login/**"]
      risk: HIGH
  ```

- [ ] **历史变更频率分析** - 经常变更的接口降权（已稳定）

**相关文件**:
- `core/src/main/kotlin/com/xxx/endpoint/core/engine/ImpactAnalyzer.kt`
- `detector-api/src/main/kotlin/com/xxx/endpoint/detector/api/model/ImpactLevel.kt`

---

#### 3.2 影响面预估报告
**状态**: 🔴 待实现  
**描述**: 生成 "此变更影响 X 个接口 → 预估影响 Y 个下游服务" 的报告  
**验收标准**:
- [ ] 统计直接影响的流量入口数量
- [ ] 估算间接影响的下游服务数量
- [ ] 生成影响拓扑图
- [ ] 计算预估测试覆盖范围

**示例输出**:
```
变更影响分析:
├── 直接影响: 3 个 HTTP 接口
│   ├── /api/v1/users (修改)
│   ├── /api/v1/orders (修改)
│   └── /api/v1/payment (高风险 - 核心链路)
├── 间接影响: 12 个下游服务
│   ├── user-service (2 个调用点)
│   ├── order-service (5 个调用点)
│   └── payment-service (3 个调用点) ⚠️ 核心
└── 建议测试范围: 15 个接口，预计耗时 2h
```

---

#### 3.3 测试建议自动生成
**状态**: 🔴 待实现  
**描述**: 基于变更范围自动生成测试用例建议  
**验收标准**:
- [ ] 识别需要回归测试的接口列表
- [ ] 根据风险级别推荐测试优先级
- [ ] 生成测试检查清单（Checklist）
- [ ] 支持导出到 Jira/TestRail 等测试管理平台

---

### Phase 4: 工作流集成 (优先级: 中)

#### 4.1 PR/MR 前置检查
**状态**: 🔴 待实现  
**描述**: Git 提交前强制检测，未通过不允许合并  
**验收标准**:
- [ ] 生成 PR Check 脚本（支持 GitHub/GitLab）
- [ ] 配置风险阈值拦截策略
- [ ] 在 PR 评论中展示检测报告
- [ ] 支持 CI/CD 集成（GitHub Actions/GitLab CI/Jenkins）

**技术方案**:
```yaml
# .github/workflows/endpoint-check.yml
name: Endpoint Detection
on: [pull_request]
jobs:
  detect:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Endpoint Detection
        uses: xxx/endpoint-detector-action@v1
        with:
          base-branch: ${{ github.base_ref }}
          risk-threshold: HIGH
```

---

#### 4.2 HTML 报告增强
**状态**: 🟡 基础 Markdown 存在，需增强  
**增强方向**:
- [ ] 交互式 HTML 报告（支持搜索、筛选、排序）
- [ ] 内嵌调用图谱可视化
- [ ] 变更代码 Diff 高亮
- [ ] 导出为 PDF
- [ ] 邮件通知模板

**相关文件**:
- `report/src/main/kotlin/com/xxx/endpoint/report/generator/`

---

#### 4.3 通知集成
**状态**: 🔴 待实现  
**描述**: 高风险变更时自动发送通知  
**验收标准**:
- [ ] 支持 Slack 通知
- [ ] 支持钉钉通知
- [ ] 支持飞书通知
- [ ] 支持邮件通知
- [ ] 可配置通知触发条件（仅 HIGH/CRITICAL）

---

### Phase 5: 体验优化 (优先级: 中)

#### 5.1 实时预览
**状态**: 🔴 待实现  
**描述**: 保存代码时自动检测，无需手动点击  
**验收标准**:
- [ ] 监听文件变更事件
- [ ] 增量检测（只检测变更部分）
- [ ] 结果实时更新到工具窗口
- [ ] 可配置自动检测开关

---

#### 5.2 快速跳转
**状态**: 🔴 待实现  
**描述**: 检测结果点击直接跳转到代码位置  
**验收标准**:
- [ ] 点击流量入口跳转到方法定义
- [ ] 点击调用链节点跳转到调用处
- [ ] 点击变更文件跳转到 Diff 视图

---

#### 5.3 配置简化
**状态**: 🟡 已有 YAML 配置，需增强  
**增强方向**:
- [ ] UI 配置界面支持可视化编辑规则
- [ ] 配置校验和自动补全
- [ ] 团队配置模板市场
- [ ] 配置版本管理

**相关文件**:
- `ui/src/main/kotlin/com/xxx/endpoint/ui/settings/`
- `core/src/main/kotlin/com/xxx/endpoint/core/config/`

---

### Phase 6: 性能优化 (优先级: 低)

#### 6.1 检测性能优化
**状态**: 🟢 当前增量检测已优化  
**进一步优化**:
- [ ] 并行检测（多线程扫描多个文件）
- [ ] 检测缓存（未变更文件结果缓存）
- [ ] 索引优化（预构建方法索引）
- [ ] 大型项目分片检测

#### 6.2 内存优化
**状态**: 🔴 待实现  
**验收标准**:
- [ ] 流式处理大型调用图
- [ ] 检测完成后释放中间对象
- [ ] 内存使用监控

---

## 优先级矩阵

| 功能 | 用户价值 | 技术难度 | 优先级 |
|------|----------|----------|--------|
| 调用图谱可视化 | ⭐⭐⭐⭐⭐ | 高 | P0 |
| 智能风险分级 V2 | ⭐⭐⭐⭐⭐ | 中 | P0 |
| 影响面预估报告 | ⭐⭐⭐⭐⭐ | 中 | P0 |
| 快速跳转 | ⭐⭐⭐⭐ | 低 | P1 |
| HTML 报告增强 | ⭐⭐⭐⭐ | 中 | P1 |
| PR Check 集成 | ⭐⭐⭐⭐ | 中 | P1 |
| 实时预览 | ⭐⭐⭐ | 中 | P2 |
| 通知集成 | ⭐⭐⭐ | 低 | P2 |
| 测试建议生成 | ⭐⭐⭐ | 高 | P2 |
| 配置简化 | ⭐⭐ | 低 | P3 |
| 性能优化 | ⭐⭐ | 高 | P3 |

---

## 里程碑规划

### Milestone 1 (v1.1.0) - 可视化 + 风险分析
- [ ] 调用图谱可视化渲染 (Mermaid/SVG)
- [ ] 智能风险分级 V2
- [ ] 影响面预估报告
- [ ] 快速跳转

### Milestone 2 (v1.2.0) - 工作流集成
- [ ] PR Check 脚本
- [ ] HTML 报告增强
- [ ] 通知集成

### Milestone 3 (v1.3.0) - 体验优化
- [ ] 实时预览
- [ ] 调用链差异对比
- [ ] 配置简化

### Milestone 4 (v2.0.0) - 高级功能
- [ ] 测试建议生成
- [ ] 性能优化
- [ ] 多语言支持

---

## 附录: 技术债务

- [ ] 解决 `buildSearchableOptions` 构建失败问题
- [ ] 移除显式 Kotlin stdlib/coroutines 依赖（IDEA 平台已提供）
- [ ] 补充单元测试覆盖率（目标 >80%）
- [ ] 统一异常处理机制
- [ ] 完善文档注释

---

*最后更新: 2026-03-13*
