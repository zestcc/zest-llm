# ZestLLM 整合型 AI 控制面与自我改进标准（完整版）

> **版本** 1.0.0 · **日期** 2026-06-10 · **用途** 实施、评审、验收的唯一参照标准  
> **读者** 架构师、后端/前端、运维、实施 AI  
> **关联** [立项交接](./ZestLLM-立项交接.md) · [Agent 配置模型](./Agent配置模型.md) · [产品验收标准](./产品验收标准.md)

---

## 0. 文档用途

本文定义 **整合型 AI 控制面** 的完整标准：不重复造 RAG/Agent/Eval 轮子，ZestLLM 做 **治理 + 发布门禁 + 闭环编排**；模型层 **只用通用基座**，「自我改进」= **RAG + Trace/反馈 + Eval + Prompt/Profile 迭代**，不做在线微调或自训基座。

**按本标准实施时须满足：**

1. 所有新能力通过 **SPI + Profile extensions** 接入，禁止 Controller 直连第三方 HTTP。
2. 推理流量仍 **不经 Control Plane 转发 token**（与立项原则一致）。
3. 任何 Profile 发布须过 **探测 + Eval 门禁**（可配置阈值）。
4. 第三方组件可 **noop / 侧车** 降级，单点故障不阻断 invoke。

---

## 1. 定位修订（与立项文档关系）

| 维度 | 立项原文 | 本标准（整合型） |
|------|----------|------------------|
| RAG / Agent 产品 | 不做通用 RAG/Agent 产品 | **不自研本体**，通过 SPI **绑定** Dify / RAGFlow 等 |
| 大模型 | 不自研 | 不变：仅 **通用 API 模型**（经 LiteLLM） |
| 控制面职责 | Prompt、路由、审计、FinOps | **+** 外部 Runtime/Knowledge 引用、Eval 闭环、蒸馏编排 |
| 业务 Agent | `@ZestLLM` + prepare/invoke/report | 不变；`runtimeMode=external` 时 execute 委托外部 Runtime |

**一句话：**

> ZestLLM = Java 控制面 + LiteLLM 网关 + Langfuse 观测 + **可插拔** Agent 编织 / 知识库 / 蒸馏桥；通用模型 + 闭环改 Prompt/知识，不改权重。

---

## 2. 设计原则

| # | 原则 | 说明 |
|---|------|------|
| P1 | 整合优先 | Dify（编织）、RAGFlow/ Dify KB（知识）、Langfuse/Braintrust（观测+Eval）、LiteLLM（模型）为默认推荐，可替换 |
| P2 | SSOT | Profile 发布版 + Provider 预设为唯一策略源；外部 App ID / Dataset ID 仅作 **引用** |
| P3 | 治理不分流 | CP 只做 prepare、策略、审计、门禁；execute 直连 LiteLLM 或外部 Runtime |
| P4 | 零自训 | 禁止依赖「业务侧微调权重」；改进仅允许：知识更新、Prompt/Profile、路由、工具、Eval 数据集 |
| P5 | 可降级 | 每个整合组件均有 `noop` 实现；Compose profile 可选启用 |
| P6 | 可验收 | 每个阶段有 AC 编号，纳入 `full-acceptance` / `e2e-acceptance` |

---

## 3. 能力三角

```text
                    ┌─────────────────┐
                    │   自我蒸馏闭环   │
                    │ Trace→Eval→改策略│
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
   ┌───────────┐      ┌───────────┐      ┌───────────┐
   │ Agent编织  │      │  知识检索  │      │ 模型网关   │
   │ Dify/Flow │      │ RAGFlow/  │      │ LiteLLM   │
   │ MCP/Tool  │      │ Dify KB   │      │ 通用模型   │
   └───────────┘      └───────────┘      └───────────┘
         │                   │                   │
         └───────────────────┴───────────────────┘
                             │
                    ┌────────▼────────┐
                    │ ZestLLM 控制面   │
                    │ Profile·探测·发布│
                    └─────────────────┘
```

| 能力 | 成熟产品（推荐） | ZestLLM 职责 |
|------|------------------|--------------|
| **编织** | Dify、Coze（私有化可选）、自建 MCP | Profile 绑定 `externalAppId`；prepare 下发 endpoint + auth ref |
| **知识** | RAGFlow、Dify Knowledge、Anyscale RAG 等 | `knowledgeRef` 引用；retrieve SPI；结果注入 Prompt 或外部 Runtime |
| **蒸馏** | Langfuse Eval、Braintrust、自建 Eval 数据集 | `learningLoop` 配置；失败样本→Eval→改 Prompt→探测→publish |
| **模型** | LiteLLM → OpenAI/Claude/DeepSeek/… | 已有 `ModelGatewayAdapter` |
| **观测** | Langfuse | 已有 `ObservabilityAdapter` |

---

## 4. 整合组件选型矩阵

| 组件 | 首选 | 备选 | 许可证 | CP 集成方式 |
|------|------|------|--------|-------------|
| 模型网关 | LiteLLM | OneAPI | MIT | 已有 |
| 可观测 | Langfuse | OTel + 自建 | MIT | 已有 |
| Agent Runtime | **Dify**（API） | FastGPT、RAGFlow Agent | Apache/各不同 | `AgentRuntimeAdapter` |
| 知识库 | **RAGFlow** | Dify Dataset、Milvus+自建 | Apache | `KnowledgeRetrievalAdapter` |
| Eval | **ZestLLM Eval** + Langfuse 导出 | Braintrust | — | Admin Eval + `LearningPipelineAdapter` |
| 策略缓存 | Valkey | Caffeine | — | 已有 |

**环境变量约定（Compose）：**

| 变量 | 说明 |
|------|------|
| `ZEST_LLM_ADAPTERS_AGENT_RUNTIME` | `native` \| `dify` \| `noop` |
| `ZEST_LLM_ADAPTERS_KNOWLEDGE_RETRIEVAL` | `noop` \| `ragflow` \| `dify-kb` |
| `ZEST_LLM_ADAPTERS_LEARNING_PIPELINE` | `noop` \| `zest-eval` |
| `DIFY_API_BASE` / `DIFY_API_KEY` | Dify 侧车 |
| `RAGFLOW_API_BASE` / `RAGFLOW_API_KEY` | RAGFlow 侧车 |

---

## 5. 总体架构

```text
  业务应用 (@ZestLLM / HTTP Client)
        │
        │ ① prepare（策略、traceId、profileVersion）
        ▼
  ┌─────────────────────────────────────────────────────────┐
  │ ZestLLM Admin (Control Plane) :8088                      │
  │  · Profile / Prompt / Route / Quota / Audit              │
  │  · Agent Probe / Eval Gate / Learning Pipeline 编排      │
  │  · SPI: Runtime · Knowledge · Gateway · Observability    │
  └───────────────┬─────────────────────┬───────────────────┘
                  │                     │
     ② execute    │                     │ 旁路上报 ③ report
                  ▼                     ▼
        ┌─────────────────┐    ┌──────────────┐
        │ runtimeMode     │    │ Langfuse     │
        │ = invoke/agent  │    │ (trace/cost) │
        │ → LiteLLM :4000 │    └──────────────┘
        │ = external      │
        │ → Dify :5001    │
        └────────┬────────┘
                 │
        ┌────────▼────────┐     ┌──────────────┐
        │ 通用 LLM API    │     │ RAGFlow      │
        │ (经 LiteLLM)    │◄────│ retrieve SPI │
        └─────────────────┘     └──────────────┘
```

**runtimeMode 语义：**

| 值 | execute 路径 | 适用场景 |
|----|--------------|----------|
| `invoke` | 业务 Agent → LiteLLM | 单轮/简单补全 |
| `agent` | 业务 Agent → LiteLLM + tools/MCP | 函数调用、MCP 预取 |
| `external` | 业务 Agent → **AgentRuntimeAdapter** | Dify 工作流、复杂 Agent |
| `hybrid` | 先 Knowledge retrieve，再 LiteLLM 或 external | RAG 增强问答 |

---

## 6. Profile 契约 v1.1（extensions 命名空间）

在 `zestllm/v1` 的 `extensions` 下增加 **稳定键**（见 `docs/schemas/profile-extensions-v1.1.json`）：

### 6.1 `extensions.runtimeBackend`

```json
{
  "type": "dify",
  "baseUrl": "http://dify:5001",
  "externalAppId": "app-uuid-from-dify",
  "protocol": "dify-chat",
  "secretRef": ".env:DIFY_API_KEY",
  "timeoutMs": 60000
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `type` | ✅ | `native` \| `dify` \| `fastgpt` \| `custom` |
| `externalAppId` | type≠native 时 ✅ | 外部应用/工作流 ID |
| `baseUrl` | ✅ | 外部 Runtime 根 URL |
| `secretRef` | 推荐 | 出站密钥，不落库明文 |

### 6.2 `extensions.knowledge`

```json
{
  "enabled": true,
  "provider": "ragflow",
  "datasetIds": ["kb-order-faq"],
  "topK": 5,
  "scoreThreshold": 0.6,
  "injectMode": "system_prefix",
  "secretRef": ".env:RAGFLOW_API_KEY",
  "baseUrl": "http://ragflow:9380"
}
```

| `injectMode` | 行为 |
|--------------|------|
| `system_prefix` | retrieve 结果拼入 system prompt（native/hybrid） |
| `external` | 仅 external runtime 消费（Dify 自带 KB 时设 external） |
| `none` | 关闭 |

### 6.3 `extensions.learningLoop`

```json
{
  "enabled": true,
  "evalDatasetRef": "order-chat-regression@v3",
  "minPassRate": 0.85,
  "probeBeforePublish": true,
  "autoDraftFromFailures": false,
  "distillationSources": ["langfuse:scores<3", "execution:guardrail_fail"],
  "reviewRequired": true
}
```

| 字段 | 说明 |
|------|------|
| `evalDatasetRef` | Admin Eval 数据集 code@version |
| `minPassRate` | publish 前 Eval 最低通过率 |
| `probeBeforePublish` | 调用 Agent Profile Probe |
| `autoDraftFromFailures` | 是否自动从失败 trace 生成 Eval case（默认 false，需人工审） |
| `reviewRequired` | true 时仅生成草稿，禁止自动 publish |

### 6.4 完整 Profile 示例

见 [profile-integration-example.json](./schemas/profile-integration-example.json)。

**runtimeMode 与 extensions 组合规则：**

| runtimeMode | runtimeBackend | knowledge | 行为 |
|-------------|----------------|-----------|------|
| `invoke` | 省略或 `native` | 可选 hybrid | CP prepare 时 retrieve，注入 prompt |
| `agent` | `native` | 可选 | 现有 MCP/tools |
| `external` | **必填** Dify 等 | `injectMode=external` 或省略 | execute 走 AgentRuntimeAdapter |
| `hybrid` | 可选 | **enabled=true** | retrieve + native LLM |

---

## 7. SPI 接口规范

配置前缀：`zest.llm.adapters`（与现有一致）。

| 配置键 | SPI 接口 | 默认实现 | 包路径 |
|--------|----------|----------|--------|
| `agent-runtime` | `AgentRuntimeAdapter` | `NativeAgentRuntimeAdapter` | `spi.runtime` |
| `knowledge-retrieval` | `KnowledgeRetrievalAdapter` | `NoopKnowledgeRetrievalAdapter` | `spi.knowledge` |
| `learning-pipeline` | `LearningPipelineAdapter` | `NoopLearningPipelineAdapter` | `spi.learning` |
| `model-gateway` | `ModelGatewayAdapter` | `LiteLLMGatewayAdapter` | 已有 |
| `observability` | `ObservabilityAdapter` | `LangfuseObservabilityAdapter` | 已有 |

### 7.1 AgentRuntimeAdapter

```java
// 职责：将 ChatRequest 委托给 Dify 等外部 Agent，返回 ChatResponse
String adapterId();
ChatResponse invoke(AgentRuntimeInvokeRequest request);
HealthStatus health();
```

- **禁止** 在 Adapter 内改 Profile 或 publish。
- **必须** 透传 `traceId` 到 Observability（由调用方或 Adapter 上报）。

### 7.2 KnowledgeRetrievalAdapter

```java
KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request);
HealthStatus health();
```

- Request 含：`datasetIds`、`query`、`topK`、`scoreThreshold`、租户/app 上下文。
- Result 含：`chunks[]`（content, score, source, metadata）。

### 7.3 LearningPipelineAdapter

```java
LearningCycleResult runCycle(LearningCycleRequest request);
List<EvalCaseSuggestion> suggestCasesFromTraces(TraceSampleQuery query);
HealthStatus health();
```

- `runCycle`：对指定 `taskCode` + 草稿 Profile 跑 Eval → 返回 passRate、失败 case、是否可 publish。
- `suggestCasesFromFailures`：从 Langfuse/Execution 抽样，**仅建议**，不自动写库（除非 `autoDraftFromFailures=true` 且 RBAC 允许）。

### 7.4 PrepareResponse 扩展字段（标准）

prepare 响应在现有字段基础上 **必须** 支持：

| 字段 | 说明 |
|------|------|
| `runtimeMode` | invoke / agent / external / hybrid |
| `runtimeBackend` | 脱敏后的 external 连接信息（无 secret） |
| `knowledgePrefetch` | hybrid 时 CP 侧已 retrieve 的摘要（可选） |
| `learningLoop` | 当前生效 loop 配置摘要（Admin 展示用） |

---

## 8. 运行时序

### 8.1 hybrid（RAG + 通用模型）

```text
Client → prepare → CP [KnowledgeAdapter.retrieve] → 注入 renderedPrompt
Client → execute → LiteLLM → report → CP → Langfuse
```

### 8.2 external（Dify）

```text
Client → prepare → CP 下发 dify baseUrl + appId + traceId
Client → execute → Dify API（或 AgentRuntimeAdapter 在 CP 内仅用于 Probe）
Client → report → CP（token/cost 若 Dify 返回则填，否则估算）
```

### 8.3 探测（Probe）

Probe **必须** 覆盖：

- `native`：现有 gateway health + 样例 invoke
- `external`：`AgentRuntimeAdapter.health()` + 样例对话（mock input）
- `knowledge`：`KnowledgeRetrievalAdapter.health()` + 空库/样例 query

---

## 9. 自我改进闭环（零自训）

```text
  ┌──────────┐    ┌──────────┐    ┌──────────────┐
  │ 线上 Trace│───►│ 样本筛选  │───►│ Eval 数据集   │
  │ Langfuse │    │ 低分/失败 │    │ (人工确认)    │
  └──────────┘    └──────────┘    └──────┬───────┘
                                         │
  ┌──────────┐    ┌──────────┐    ┌──────▼───────┐
  │ Publish  │◄───│ Probe OK │◄───│ 改 Prompt/   │
  │ 新版本   │    │ Eval≥阈值│    │ Profile/KB   │
  └──────────┘    └──────────┘    └──────────────┘
```

| 步骤 | 执行者 | 产物 |
|------|--------|------|
| 1. 采集 | Langfuse + Execution | traceId、score、guardrail 失败 |
| 2. 策展 | Admin / LearningPipeline | Eval case 草稿 |
| 3. 迭代 | 人工或 Playground | Profile 新 **草稿** 版本 |
| 4. 验证 | Eval + Probe | passRate、latency、schema |
| 5. 发布 | Admin publish | PUBLISHED + 缓存失效 |
| 6. 监控 | Dashboard / FinOps | 回归告警 |

**明确禁止：**

- 在线更新模型权重、LoRA、业务侧 fine-tune 管道接入 CP。
- 未过 Eval 的自动 publish（`reviewRequired=false` 仅限非生产租户）。

---

## 10. 发布门禁

Profile `publish` API **必须** 依次检查（可配置跳过，生产禁止跳过）：

| 顺序 | 检查项 | 失败 HTTP |
|------|--------|-----------|
| G1 | JSON Schema / extensions 合法 | 400 |
| G2 | `learningLoop.probeBeforePublish` → Probe 全绿 | 409 `PROBE_FAILED` |
| G3 | `learningLoop` Eval passRate ≥ minPassRate | 409 `EVAL_BELOW_THRESHOLD` |
| G4 | 审计 + RBAC | 403 |

**与现有 AC 关系：** AC9（import/publish）、AC36（probe）保留；新增 **AC39–AC44**（见 §14）。

---

## 11. Docker Compose 拓扑

文件：`deploy/docker-compose.integration.yml`（可选 profile `integration`）。

```text
services:
  mysql, valkey, litellm, langfuse   # 已有
  dify-api, dify-worker, dify-web    # 侧车
  ragflow                            # 侧车
  zest-llm-admin                     # 挂载 integration env
```

Admin 环境示例：

```yaml
zest:
  llm:
    adapters:
      agent-runtime: dify
      knowledge-retrieval: ragflow
      learning-pipeline: zest-eval
      observability: langfuse
      model-gateway: litellm
```

**网络：** Admin 与侧车同 compose network；业务 Demo **不** 强制依赖 Dify（native 模式可独立验收）。

---

## 12. Admin UI 能力清单

| 模块 | 必做项 | 阶段 |
|------|--------|------|
| 智能体配置 | extensions 表单：Runtime / Knowledge / LearningLoop | M1 |
| 智能体配置 | 外部 App ID 校验（Probe 按钮） | M1 |
| Eval | 数据集 CRUD + 批量跑 + 通过率 | 已有，接 publish 门禁 M2 |
| 智能体配置 | Publish 前展示 Eval/Probe 结果 | M2 |
| 运维 | Learning 周期任务历史 | M3 |
| Dashboard | 外部 Runtime / KB 健康 | M1 |

---

## 13. 分阶段实施路线图

| 阶段 | 周期 | 交付 | 退出标准 |
|------|------|------|----------|
| **M0** | 已完成 | LiteLLM、Langfuse、Profile、Probe、Eval 基础 | full-acceptance 43/0/0 |
| **M1** | 2 周 | SPI 三接口 + noop/health；Profile extensions 解析；Prepare 扩展；Compose integration 骨架 | AC39–AC41 |
| **M2** | ✅ 已实现 | Dify AgentRuntimeAdapter；RAGFlow KnowledgeAdapter；publish Eval/Probe 门禁 | AC40–AC43 · `integration-demo.sh` |
| **M3** | 2 周 | LearningPipeline 周期任务；Langfuse 失败样本导入 Eval；Admin UI 完整 | AC44 |
| **M4** | 可选 | Braintrust 导出、多 Runtime 路由 | 按需 |

---

## 14. 验收标准（AC39–AC44）

| ID | 场景 | 通过标准 |
|----|------|----------|
| AC39 | Profile extensions 解析 | import 含 `runtimeBackend`/`knowledge`/`learningLoop` 无 500 |
| AC40 | Prepare hybrid | `runtimeMode=hybrid` 时响应含 `knowledgePrefetch` 或 injected prompt |
| AC41 | SPI health | `GET /api/admin/adapters/health` 含 agent-runtime、knowledge-retrieval、learning-pipeline |
| AC42 | Dify external | `runtimeMode=external` + Dify 配置，Probe 样例对话 PASS |
| AC43 | Publish 门禁 | Eval 低于阈值时 publish 返回 409 `EVAL_BELOW_THRESHOLD` |
| AC44 | Learning 建议 | `suggestCasesFromTraces` API 返回样本列表且不自动 publish |

脚本：扩展 `deploy/scripts/full-acceptance.ps1` 与 `e2e-acceptance.sh`（M1 起）；B 整合严格验收见 `run-integration-acceptance.sh`（`ZEST_INTEGRATION_E2E=1`）。

---

## 15. 与现有代码映射

| 本标准 | 现有实现 | 差距 |
|--------|----------|------|
| ModelGateway | `LiteLLMGatewayAdapter` | ✅ |
| Observability | `LangfuseObservabilityAdapter` | ✅ |
| Profile SSOT | `llm_agent_profile` + Admin | ✅ |
| Agent Probe | `AdminAgentProfileProbeController` + `AgentProfileProbeService` | ✅ external-runtime / knowledge-health / DifyKb per-profile health |
| Eval | Admin Eval CRUD + 批量 invoke + publish 门禁 | ✅ AC43 |
| AgentRuntime | `DifyAgentRuntimeAdapter` | ✅ M2 |
| Knowledge | `RagflowKnowledgeRetrievalAdapter` · `DifyKbKnowledgeRetrievalAdapter` | ✅ M2 |
| LearningLoop | `LearningPipelineAdapter` + suggest-cases | ✅ M3 AC44 · 自动发布可选 |

**SPI 源码位置（契约）：**

- `zest-llm-spi/.../runtime/AgentRuntimeAdapter.java`
- `zest-llm-spi/.../knowledge/KnowledgeRetrievalAdapter.java`
- `zest-llm-spi/.../learning/LearningPipelineAdapter.java`
- `zest-llm-spi/.../profile/RuntimeBackendConfig.java` 等

---

## 16. 附录

### 16.1 Admin API（增量）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/agent-profiles/{taskCode}/publish` | 增加 G2/G3 门禁 |
| POST | `/api/admin/learning/suggest-cases` | 从 trace 建议 Eval case |
| POST | `/api/admin/learning/run-cycle` | 对草稿跑完整闭环 |
| GET | `/api/admin/adapters/health` | 扩展三类 SPI |

### 16.2 配置项完整列表

```yaml
zest:
  llm:
    adapters:
      model-gateway: litellm
      observability: langfuse
      agent-runtime: native          # native | dify | noop
      knowledge-retrieval: noop      # ragflow | dify-kb | noop
      learning-pipeline: noop        # zest-eval | noop
      policy-cache: caffeine
      quota: db
```

### 16.3 术语表

| 术语 | 定义 |
|------|------|
| 自我蒸馏 | 从 trace/反馈提炼 Eval 样本，改进 Prompt/KB，非训练模型 |
| 控制面 | ZestLLM Admin，不转发 token 流 |
| 外部 Runtime | Dify 等完整 Agent 执行环境 |
| hybrid | CP 侧 RAG 预取 + LiteLLM 生成 |

---

**变更记录**

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-06-10 | 完整版首版：整合架构 + Profile v1.1 + SPI + 门禁 + AC39–44 |
