# ZestLLM 专项立项交接文档

> **版本** 0.1.0 · **日期** 2026-06-09 · **用途** 交给实施 AI / 开发团队，独立立项  
> **许可证** Apache-2.0 · **项目名** ZestLLM · **仓库建议** `zest-llm`

---

## 1. 项目是什么

**ZestLLM** 是一个 **AI 作业调度与治理平台**（LLM Control Plane）。

类比：XXL-Job 管「任务调度」，ZestLLM 管「AI 调用调度与治理」。

**核心能力：**

- 多模型统一接入（OpenAI / Claude / DeepSeek / 通义 / 本地 Ollama 等）
- Prompt 版本管理与发布
- 模型路由、限流、熔断、降级（fallback）
- 调用审计、成本统计、traceId 全链路
- Java 业务侧 `@ZestLLM` 注解接入

**明确不做：**

- 不做业务 LLM 应用（客服、销售话术、行业规则等）
- 不自研大模型 / 推理引擎
- 不做通用 Agent / RAG 产品
- 不做 ChatBot 平台

---

## 2. 一句话定位

> **业务声明 AI 作业（code），平台统一执行 Prompt 匹配、模型调用、结果回填与治理。**

---

## 3. 架构原则

```text
治理中心化，执行分布式。
```

| 层级 | 职责 | 是否承载推理流量 |
|------|------|------------------|
| **Control Plane** | 鉴权、Prompt、路由策略、配额、审计 | ❌ 轻量 |
| **Agent（业务侧）** | 本地缓存 Prompt/Policy，直连模型网关 | 中转，不解析 |
| **LiteLLM 集群** | 多模型 API 统一、fallback | ✅ 重流量 |
| **Langfuse** | Trace、成本、Prompt 实验 | 旁路 |

**禁止：** 所有 token / 流式响应都经过 Control Plane 转发（会成为瓶颈）。

**推荐三段式（v0.2 目标）：**

1. `prepare` — 业务 Agent → Control Plane（拿 traceId、promptVersion、routePolicy）
2. `execute` — 业务 Agent → LiteLLM（实际推理）
3. `report` — 业务 Agent → Control Plane 异步上报（output、token、cost）

**MVP（v0.1）可简化：** 单一接口 `POST /v1/llm/invoke` 跑通后再拆。

---

## 4. 业务接入模型

### 4.1 简单任务（单 AI，同步）

```java
@ZestLLM(code = "aiChat", timeoutMs = 30000, retry = 1)
public AiChatResult aiChat(@AiInput("question") String question,
                           @AiOutput AiChatResult result) {
    // 进入方法时，result 已被框架填入 AI 初值（answer/confidence 等）
    // 业务在此做二次加工（每个业务不同）
    if (result.getConfidence() < 0.8) {
        result.setNeedManualReview(true);
    }
    return result;
}
```

```java
// A 方法
public OrderView methodA(Long orderId, String question) {
    AiChatResult ai = orderAiFacade.aiChat(question);
    return OrderView.from(orderId, ai);
}
```

### 4.2 调用链

```text
A 方法
  → 调用 @ZestLLM 方法
  → Starter 拦截
  → Control Plane（匹配 prompt / 路由模型 / 生成 traceId）
  → LiteLLM 调模型
  → 框架把 AI 输出映射到 @AiOutput Result
  → 执行业务方法体（后处理）
  → 返回给 A 方法
```

### 4.3 注解语义

| 注解 | 作用 |
|------|------|
| `@ZestLLM(code)` | AI 作业码，对应平台 Prompt 配置 |
| `@AiInput("name")` | 输入变量，映射到 Prompt 模板 |
| `@AiOutput` | 输出容器：AI 初值 + 业务扩展字段 |
| `@AiContext("name")` | 业务上下文（可选，默认不进 Prompt） |

### 4.4 Result 字段约定

- **AI 区**（answer、confidence、tags）：由平台 outputSchema 自动填入，建议只读
- **业务区**（bizId、needManualReview）：方法体写入

---

## 5. 复杂任务（多 AI 协调）

简单任务：`@ZestLLM` 单步即可。

复杂任务（如：任务解释 AI → 数据处理 AI → 画图 AI → 文档生成 AI）：

- **推荐**：接入外部 **业务流程编排器**，ZestLLM 提供 **AI 节点执行器**
- ZestLLM 本身 **不负责 DAG 编排**，只负责每个 AI 节点的单次 invoke
- 编排器通过 `ZEST_LLM` 节点类型调用 `POST /v1/llm/invoke`，节点间上下文由编排器传递

```text
编排器 Flow
  → AI 节点 1：code=taskExplain
  → AI 节点 2：code=dataProcess（input 来自上一步 output）
  → AI 节点 3：code=imageGen（条件分支）
  → AI 节点 4：code=docGen
  → 业务节点：落库
```

**可选集成：** 作者另有 Java 编排器产品（方法级 `@ZestExecute` + DAG），可通过 `zest-llm-flow-adapter` 模块对接，**不是硬依赖**。

---

## 6. 开源技术选型

**原则：** 主项目 Apache-2.0；依赖优先 MIT / Apache-2.0 / BSD；底座可插拔替换。

| 组件 | 用途 | 许可证 | 备注 |
|------|------|--------|------|
| Java 17 + Spring Boot 3.2 | 主框架 | Apache-2.0 | |
| **LiteLLM Proxy** | 模型网关 | MIT | 默认；可换 OneAPI |
| **Langfuse** | Trace / 成本 | MIT | 可 self-host |
| **MySQL** | 元数据 | GPL（社区版） | 8.0+，与 ZestFlow 栈统一 |
| **Valkey** | 缓存 | BSD-3 | 推荐；或 Redis ≤7.2 |
| Apache Kafka | 异步 report（可选） | Apache-2.0 | |
| Aviator | 条件/路由表达式 | Apache-2.0 | |
| springdoc-openapi | API 文档 | Apache-2.0 | |

**Redis 说明：** 开源项目可用 Redis；Redis 7.4+ 许可证非 OSI 开源，发行版默认推荐 Valkey 或 Redis 7.2。

**明确不引入：** AGPL 组件、SSPL 数据库作为核心依赖。

---

## 7. 仓库结构

```text
zest-llm/
├── LICENSE                          # Apache-2.0
├── NOTICE                           # 第三方许可证
├── README.md
├── pom.xml
├── docs/
│   ├── 系统设计文档.md
│   ├── openapi.yaml
│   └── THIRD_PARTY_LICENSES.md
├── zest-llm-common/                 # DTO、错误码、Trace 上下文
├── zest-llm-control-plane/          # 治理 API 服务
├── zest-llm-agent/                  # 业务侧 Agent（缓存 + 直连 LiteLLM）
├── zest-llm-starter/                # @ZestLLM 注解 + AOP + AutoConfig
├── zest-llm-flow-adapter/           # 可选：外部编排器 AI 节点适配
├── zest-llm-demo/
└── deploy/
    └── docker-compose.yml           # PG + Valkey + LiteLLM + Langfuse + CP
```

**Maven 坐标建议：** `cn.zest.www:zest-llm`

---

## 8. 核心对象模型

| 对象 | 说明 |
|------|------|
| **Tenant** | 租户 |
| **App** | 业务应用（appKey，如 order-service） |
| **AiTaskDef** | AI 作业定义，由 `code` 标识 |
| **PromptVersion** | Prompt 模板版本（DRAFT / PUBLISHED / DEPRECATED） |
| **ModelRoute** | 路由策略（cost_first / quality_first + fallback） |
| **Execution** | 单次调用记录（traceId、token、cost、status） |

---

## 9. API 设计（MVP）

### 9.1 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/llm/invoke` | **MVP 合一接口** |
| POST | `/v1/llm/prepare` | v0.2：拿 prompt + 路由 |
| POST | `/v1/llm/report` | v0.2：异步上报结果 |
| POST | `/v1/registry/methods` | Starter 启动注册 `@ZestLLM` 元数据 |
| GET | `/v1/tasks/{code}` | 查询 AiJob |
| POST | `/v1/prompts/{code}/publish` | 发布 Prompt 版本 |
| GET | `/v1/executions/{traceId}` | 查询执行记录 |

### 9.2 invoke 请求示例

```json
{
  "appKey": "order-service",
  "code": "aiChat",
  "bizId": "order-1001",
  "inputs": { "question": "这个订单为什么延迟？" },
  "context": { "tenantId": "t001", "userId": "u123" },
  "options": { "timeoutMs": 30000, "retry": 1 }
}
```

### 9.3 invoke 响应示例

```json
{
  "traceId": "tr_abc123",
  "status": "SUCCESS",
  "code": "aiChat",
  "promptVersion": "v3",
  "model": "deepseek/deepseek-chat",
  "output": {
    "answer": "由于仓库缺货导致延迟",
    "confidence": 0.91,
    "tags": ["delay", "stockout"]
  },
  "metrics": {
    "latencyMs": 842,
    "promptTokens": 120,
    "completionTokens": 45,
    "cost": 0.0021
  }
}
```

### 9.4 错误码

| 错误码 | 说明 |
|--------|------|
| `AUTH_FAILED` | 鉴权失败 |
| `QUOTA_EXCEEDED` | 配额超限 |
| `PROMPT_NOT_FOUND` | 无已发布 Prompt |
| `MODEL_TIMEOUT` | 模型超时（含 fallback 失败） |
| `OUTPUT_SCHEMA_MISMATCH` | 输出不符合 schema |

---

## 10. 数据库表（8 张）

```text
llm_tenant           租户
llm_app              业务应用（appKey + tokenHash）
llm_ai_task_def      AI 作业定义（app_id + code 唯一）
llm_prompt_version   Prompt 版本（template + output_schema + status）
llm_model_route      模型路由（primary + fallback + policy）
llm_execution        执行记录（traceId + input/output + token + cost）
llm_method_registry  Starter 上报的方法元数据
llm_app_quota        配额（daily_token_limit + qps_limit）
```

---

## 11. Starter AOP 伪代码

```java
@Around("@annotation(zestLLM)")
public Object around(ProceedingJoinPoint pjp, ZestLLM zestLLM) {
    InvokeRequest req = buildRequest(pjp, zestLLM);
    InvokeResponse resp = llmControlClient.invoke(req);

    if (!resp.isSuccess()) {
        throw new ZestLlmException(resp.getErrorCode(), resp.getTraceId());
    }

    Object[] args = pjp.getArgs();
    Object outputBean = findAiOutputArg(args);
    aiResultMapper.mapToOutput(resp.getOutput(), outputBean);

    return pjp.proceed(args); // 执行业务后处理，返回最终 Result
}
```

---

## 12. 配置示例

```yaml
zest:
  llm:
    enabled: true
    control-plane-url: http://localhost:8088
    app-key: order-service
    auth-token: ${ZEST_LLM_APP_TOKEN}
    agent:
      enabled: true
      cache-ttl: 300s
    litellm-url: http://localhost:4000
    registry-on-startup: true
```

---

## 13. MVP 路线图

### v0.1 — 单 AI 同步（3~4 周）

- [ ] 创建仓库，Apache-2.0 + docker-compose（PG + Valkey + LiteLLM）
- [ ] Control Plane：`POST /v1/llm/invoke` 打通 LiteLLM
- [ ] 手工配置 `aiChat` Prompt v1
- [ ] `@ZestLLM` + `@AiInput` / `@AiOutput` + AOP
- [ ] Execution 落库 + traceId
- [ ] Demo：`methodA → aiChat → return`

### v0.2 — 薄层化 + 多 AI（4~6 周）

- [ ] 拆 prepare / execute / report
- [ ] `zest-llm-agent` 本地缓存
- [ ] 外部编排器 `ZEST_LLM` 节点适配（可选模块）
- [ ] Langfuse 接入
- [ ] 4 步 AI Demo 链（explain → process → draw → doc）

### v0.3 — 治理增强（4 周）

- [ ] Prompt 发布 / 回滚 Admin API
- [ ] 配额 / 限流
- [ ] 成本看板（按 app / code / model）
- [ ] 模型分级策略

---

## 14. 验收 KPI

| 指标 | 目标 |
|------|------|
| 业务接入时长 | ≤ 0.5 天 |
| Prompt 变更 | 不发业务版 |
| 调用可追溯 | 100% 有 traceId |
| Control Plane P95（不含推理） | < 50ms |
| fallback 成功率 | > 95% |

---

## 15. 适配器接口（可插拔）

```java
public interface ModelGatewayAdapter {
    ChatResponse chat(ChatRequest request);
    HealthStatus health();
}

public interface ObservabilityAdapter {
    void trace(TraceEvent event);
}

public interface PromptRenderer {
    String render(PromptTemplate template, Map<String, Object> variables);
}
```

默认：`LiteLLMGatewayAdapter` + `LangfuseObservabilityAdapter`。

---

## 16. 性能与降级

| 风险 | 对策 |
|------|------|
| CP 成瓶颈 | 三段式；CP 无状态水平扩展 |
| 推理慢 | 直连 LiteLLM；流式不经过 CP |
| 审计写入慢 | report 异步 + MQ 削峰 |
| CP 不可用 | Agent 本地缓存有效时允许执行 + 后补 report |
| 主模型失败 | fallback 模型；仍失败返回 `MODEL_TIMEOUT` |

---

## 17. 实施 AI 的第一步

1. 创建 `zest-llm` 仓库，Apache-2.0
2. 初始化 Maven 多模块（见 §7）
3. `deploy/docker-compose.yml` 拉起 PG + Valkey + LiteLLM
4. 实现 Control Plane 最小 `POST /v1/llm/invoke`
5. 实现 Starter `@ZestLLM` 最小 AOP
6. Demo 跑通：`methodA → aiChat → return`

**先跑通同步单 AI，再做编排器集成和治理增强。**

---

## 18. 竞品参考（不依赖）

| 方案 | 借鉴点 | ZestLLM 差异 |
|------|--------|-------------|
| LiteLLM | 模型网关 | + 企业治理 + Java 注解 |
| Langfuse | Trace / Prompt | + 运行时调度 |
| Spring AI | Java SDK | + Control Plane，不做 SDK 竞争 |
| XXL-Job | 注册 / 治理心智 | 面向 AI 非 Cron |

---

## 19. 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 0.1.0 | 2026-06-09 | 独立立项交接稿 |
