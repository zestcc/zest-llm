# Zest Stack 完整实现方案

> **定位**：ZestLLM = Java 生态的 **XXL-Job for AI** —— A 层选能力栈，B 层配 AI 作业，业务 `@ZestLLM(code)` 定作业，平台统一调度与治理。

## 1. 目标与范围

| 维度 | 目标 |
|------|------|
| 规模 | 小型（单机/POC）、中型（部门级）、大型（多租户/高并发）均可部署 |
| 可靠性 | 发布门禁（Eval/Probe）、执行审计、Probe 巡检、失败样本闭环 |
| 性能 | prepare P95 ≤ 800ms（冒烟）；中型栈 Valkey 缓存；大型水平扩展 Admin |
| 并发 | Runtime 无状态；LiteLLM 网关分流；连接池与超时可配置 |

## 2. 部署分层（Zest Stack Tier）

### 2.1 小型（small）— POC / 1～2 个 AI 作业

```bash
# Windows
.\deploy\scripts\zest-stack-up.ps1 -Tier small

# Linux / macOS
bash deploy/scripts/zest-stack-up.sh small
```

**组件**：MySQL + LiteLLM + openai-mock + Admin + Demo + mcp-mock  
**适配器**：`model-gateway=litellm`，`observability=noop`，`agent-runtime=native`  
**适用**：本地开发、功能验收、单 code 试点  

### 2.2 中型（medium）— 部门级 / 5～20 个作业

```bash
.\deploy\scripts\zest-stack-up.ps1 -Tier medium
```

**组件**：small + Valkey + Langfuse（observability profile）  
**适配器**：`observability=langfuse`，`response-cache=valkey`，`learning-pipeline=zest-eval`  
**适用**：多 code、FinOps、Probe 告警、Eval 发布门禁  

### 2.3 大型（large）— 企业 / 高并发 / 整合栈

```bash
.\deploy\scripts\zest-stack-up.ps1 -Tier large
```

**组件**：medium + Kafka report + Dify/RAGFlow（integration profile）  
**适配器**：`agent-runtime=dify`，`knowledge-retrieval=ragflow`，`report-channel=kafka`  
**适用**：多步 Agent、RAG、异步 report、MCP 工具链  

### 2.4 规模对照

| 指标 | 小型 | 中型 | 大型 |
|------|------|------|------|
| 预期 QPS（prepare） | 50 | 200 | 500+（多 Admin 副本） |
| 缓存 | Caffeine | Valkey | Valkey + 响应缓存 |
| 可观测 | 内置 Execution | Langfuse | Langfuse + 归档 |
| Agent 编排 | native | native/hybrid | Dify external |
| 知识库 | 无 | hybrid noop | RAGFlow |

## 3. 产品分层（A / B / 业务）

```
┌─────────────────────────────────────────────────────────┐
│  Admin 门户                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ A 能力栈     │  │ B 场景模板   │  │ AI 作业看板  │  │
│  │ /capability  │  │ /scenarios   │  │ /tasks       │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────┬──────────────────────────────┘
                           │ prepare / invoke / report
┌──────────────────────────▼──────────────────────────────┐
│  Agent 栈：LiteLLM · Dify · RAGFlow · Langfuse · MCP    │
└──────────────────────────┬──────────────────────────────┘
                           │ @ZestLLM(code)
┌──────────────────────────▼──────────────────────────────┐
│  业务 Java 应用（Starter / Agent / Flow）                │
└─────────────────────────────────────────────────────────┘
```

### 3.1 A 层 — 能力栈（`/api/admin/capability-stack`）

- 展示当前 Tier、各 SPI 健康、推荐 compose 命令
- 预置 small / medium / large 三套栈定义

### 3.2 B 层 — 场景模板（`/api/admin/scenario-templates`）

| 模板 ID | 场景 | 推荐 Tier |
|---------|------|-----------|
| `chat-basic` | 智能对话 | small+ |
| `report-basic` | 报表解读 | medium+ |
| `ops-monitor` | 运维诊断 | large（MCP + hybrid） |

一键应用：创建 Task + 导入 Profile 草稿（可选发布）。

### 3.3 业务层 — `@ZestLLM`

不变；业务只声明 code，策略由 Profile SSOT 驱动。

## 4. 可靠性设计

| 机制 | 说明 |
|------|------|
| 发布门禁 | Eval 通过率 + Probe 通过才允许 publish（409） |
| Probe 巡检 | 每 30 分钟 + 手动 run-all |
| 执行审计 | llm_execution 全链路 traceId |
| 失败样本 | ExecutionSample → Learning suggest-cases |
| 降级 | Profile model.fallback 链 |
| 超时 | generation.timeoutMs + 网关超时 |

## 5. 性能与高并发

| 层级 | 手段 |
|------|------|
| Runtime | Admin 无状态，可水平扩展（K8s replicas） |
| 策略缓存 | policy-cache（Caffeine/Valkey） |
| 响应缓存 | response-cache（可选 Valkey） |
| 网关 | LiteLLM 连接池 + 多模型路由 |
| DB | MySQL 索引 + Execution 归档（V14+） |
| 压测 | `stress-test-prepare.ps1` — 并发 prepare，输出 P50/P95/QPS |

**性能指标（验收）**：

- prepare P95 ≤ 800ms（30 样本冒烟）
- 压测 100 并发成功率 ≥ 95%
- Admin API 列表页 P95 ≤ 500ms

## 6. 测试体系

### 6.1 单元测试

- `*Test.java` — Service / Validator / Adapter 逻辑
- 新增：`ScenarioTemplateServiceTest`、`CapabilityStackServiceTest`、`AiJobOverviewServiceTest`

### 6.2 集成测试

- `*IT.java` — Testcontainers MySQL + WireMock
- Runtime prepare/invoke 链路

### 6.3 验收测试

- `full-acceptance.ps1` — 功能 + 安全 + 性能冒烟 + AC39–48
- `e2e-acceptance.sh` — Docker 全栈 AC1–38

### 6.4 性能 / 压力测试

- `loadtest-cp-prepare.sh` — 单线程批量
- `stress-test-prepare.ps1` — 多并发 Job

### 6.5 执行命令

```powershell
# 单元 + 集成
mvn clean test

# 本地 Admin 全量验收（需 :8088）
.\deploy\scripts\full-acceptance.ps1

# 压力冒烟
.\deploy\scripts\stress-test-prepare.ps1 -Concurrency 50 -Total 200
```

## 7. 目录与交付物

```
deploy/scripts/zest-stack-up.{ps1,sh}   # 分层一键部署
deploy/scripts/stress-test-prepare.ps1  # 压力测试
examples/scenarios/                     # 场景 Profile 样例
examples/mcp/                           # MCP 接入说明与 mock 指引
zest-llm-admin-ui/.../CapabilityStackView.vue
zest-llm-admin-ui/.../ScenarioTemplatesView.vue
```

## 8. 与 Spring AI 关系

- **Spring AI**：应用内 SDK，统一 Model/Tool API
- **ZestLLM**：应用外控制面，Profile/Eval/Probe/FinOps
- **推荐**：业务 `@ZestLLM` + 平台选 Dify/LiteLLM；Spring AI 可作为应用内补充，非替代

## 9. 实施里程碑

| 阶段 | 内容 | 状态 |
|------|------|------|
| M1 | Prepare 扩展 + adapters health | ✅ |
| M2 | Dify/RAGFlow + Publish 门禁 | ✅ |
| M3 | Learning Pipeline + Admin UI | ✅ |
| M4 | Zest Stack 分层部署 + 能力栈/场景模板/作业看板 | 本文档 + 代码 |
| M5 | Langfuse→Eval 自动导入 + Chat UI（可选） | 规划中 |

---

**验收通过标准**：`mvn test` 全绿 + `full-acceptance.ps1` 0 FAIL + 压测指标达标。
