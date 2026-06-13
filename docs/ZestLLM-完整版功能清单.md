# ZestLLM 完整版功能清单

> **版本** 1.0.0 · **日期** 2026-06-13 · **适用范围** 控制面完整版（本地 / Docker small～medium / CI 门禁）  
> **数据库** MySQL 8 only · **Flyway 最新** V22 · **Admin 应用版本** 1.0.0

---

## 1. 「完整版」定义

在本仓库语境下，**完整版**指：

1. **控制面（Admin CP）** 功能齐备：治理、配置、运维、Zest Stack 门户、M5 场景/Wizard/Learning 等 API 与 UI 可用。  
2. **Runtime 契约** 完整：`prepare` / `invoke` / `report`（含流式）+ 鉴权 + Execution 审计。  
3. **本地可一键启动 + 自动化验收**：`start-local-full` + `verify-local`，**66+ 项** API 验收（`full-acceptance.ps1` 内 `Assert-Pass`）+ `mvn test` 通过。  
4. **交付物齐全**：Docker Compose、Helm Chart、CI 门禁、文档与验收脚本对齐 **AC1–AC53**。

**不包含**（单独立项或下一阶段）：

| 项 | 说明 |
|----|------|
| 生产环境正式签字 | 需真实 K8s/Helm 部署、压测与变更流程 |
| Large Tier 日常门禁 | Dify + RAGFlow + Kafka 全栈需 `zest-stack-up -Tier large` 单独验证 |

---

## 2. 代码模块

| 模块 | 职责 |
|------|------|
| `zest-llm-spi` | 可插拔适配器 SPI（Gateway / Observability / Quota / Cache / Runtime / Knowledge / Learning 等） |
| `zest-llm-common` | 公共 API、错误码、prepare/report 契约 |
| `zest-llm-infra` | LiteLLM、Handlebars、Caffeine/Valkey、Guardrails、MCP、Dify/RAGFlow 等默认实现 |
| `zest-llm-admin` | Control Plane + Admin API + 内嵌静态 UI |
| `zest-llm-admin-ui` | Vue3 Admin 前端（Vite） |
| `zest-llm-starter` | `@ZestLLM` 注解 + AOP（invoke / agent 双模式） |
| `zest-llm-agent` | 业务侧 prepare → execute(LiteLLM) → report |
| `zest-llm-flow-adapter` | ZestFlow `ZEST_LLM` 节点执行器 |
| `zest-llm-demo` | 业务接入示例（order-service） |

---

## 3. Admin 门户（UI 页面）

登录：`admin` / `admin123`（本地默认）。支持内嵌 UI（`:8088`）或 Vite dev（`:5174`）。

| 路由 | 页面 | 主要能力 |
|------|------|----------|
| `/login` | 登录 | JWT 本地账号 + 可插拔 SSO（ZestSSO/OIDC） |
| `/dashboard` | 概览 | 调用统计、成本趋势、智能体健康、Gateway/Adapter 状态 |
| `/capability-stack` | 能力栈 | 当前 Tier、SPI 清单、分层部署说明、导出 compose 片段 |
| `/scenario-templates` | 场景模板 | 浏览/应用内置模板（chat / report / ops） |
| `/apps` | 应用管理 | App CRUD、Token 轮换、配额、Auth 绑定、App 总览 |
| `/tenants` | 租户管理 | 租户列表与创建 |
| `/tasks` | AI 作业 | Task CRUD、与 Prompt/Profile 关联 |
| `/prompts` | Prompt 管理 | 版本管理、发布、Diff |
| `/playground` | Playground | Prompt 预览、Admin 内 Invoke 试跑 |
| `/eval` | Eval 评测 | 数据集 CRUD、批量 run、通过率报表 |
| `/learning` | 自我改进 | 失败样本建议、Learning Cycle、多源蒸馏 |
| `/flow-chains` | Flow 链 | ZestFlow 链注册表只读浏览 |
| `/model-routes` | 模型路由 | 路由策略列表 |
| `/agent-config` | 智能体配置 | Profile 版本、Provider 预设、MCP、探测/历史/对比、extensions 表单 |
| `/users` | 用户管理 | Admin 用户 CRUD |
| `/executions` | 执行记录 | 分页查询、traceId 详情、Langfuse 链接 |
| `/registry` | 方法注册 | Starter 上报的 `@ZestLLM` 方法元数据 |
| `/audit-logs` | 审计日志 | 操作留痕查询 |
| `/ops` | 运维中心 | 成本告警、Probe 告警、归档统计入口 |
| `/adapters` | 适配器健康 | 全 SPI 健康探测 |

---

## 4. Admin API（按域）

前缀：`/api/admin`（除特别说明）。Swagger：`/swagger-ui.html`。

### 4.1 元信息与认证

| API | 说明 |
|-----|------|
| `GET /meta/features` | 特性开关、schema 就绪、Flyway 版本 |
| `GET /meta/build` | 构建信息（appVersion、gitCommit、buildTime、activeProfiles） |
| `POST /auth/login` | 本地 JWT 登录 |
| `GET/POST /auth/sso/*` | 可插拔 SSO（ZestSSO / OIDC / PKCE） |
| `GET/POST /auth/oidc/*` | Legacy 别名，与 `/auth/sso/*` 等价 |

### 4.2 治理基础

| 域 | 主要端点 |
|----|----------|
| 租户 | `GET/POST /tenants` |
| 应用 | `GET /apps`、`GET /apps/overview`、`POST /apps`、`POST /apps/{appKey}/rotate-token` |
| 配额 | `GET /apps/{appKey}/quota` |
| Auth 绑定 | App 级 OIDC_JWT / STATIC_TOKEN 配置 |
| 用户 | `GET/POST /users` |
| AI 作业 | `GET/POST /tasks`、Task CRUD |
| Prompt | 版本列表、创建、发布、Diff |
| 模型路由 | `GET /model-routes` |
| Provider 预设 | `GET/POST /provider-presets` |
| 审计 | `GET /audit-logs` |
| 方法注册 | Starter Registry 上报查询 |

### 4.3 智能体 Profile（SSOT）

| 能力 | 端点示例 |
|------|----------|
| 版本 CRUD | `GET/POST /agent-profiles/{taskCode}/versions` |
| 发布/回滚 | `POST .../publish`、`GET .../publish-preview`、`POST .../rollback` |
| 导入/导出 | `POST /import`、`GET .../export` |
| Provider 切换 | `POST .../activate-provider` |
| Diff | `GET .../diff` |
| Extensions | runtimeBackend / knowledge / learningLoop（校验含 native 免 baseUrl） |
| 发布门禁 | Eval 通过率 + Probe → 409 `EVAL_BELOW_THRESHOLD` 等 |

### 4.4 智能体探测（Probe）

| 能力 | 端点 |
|------|------|
| 单次/版本探测 | `POST /agent-profiles/{taskCode}/probe`、`.../versions/{version}/probe` |
| 批量巡检 | `POST /agent-profiles/probe-all` |
| 新路径 API | `/agent-profile-probes/{taskCode}/run`、`/latest`、`/history`、`/history/trend`、`/compare`、`/history/export` |
| 告警 | `GET /agent-probe-alerts`、`POST .../resend` |
| 定时巡检 | 可配置 schedule（本地 profile 默认关闭） |

### 4.5 执行与可观测

| 能力 | 端点 |
|------|------|
| 执行分页/详情 | `GET /executions`、`GET /executions/{traceId}` |
| 归档 | `GET /executions/archive/stats`、`.../archive/runs` |
| 观测配置 | `GET /config/observability` |
| Dashboard | `GET /dashboard/stats`、`/dashboard/cost`、`/dashboard/agent-health` |

### 4.6 FinOps

| 能力 | 端点 |
|------|------|
| 成本告警 | `GET /cost-alerts`、`GET /cost-alerts/summary` |
| Webhook | 配额策略 + HttpAlertWebhookAdapter |

### 4.7 Eval 与学习闭环

| 能力 | 端点 |
|------|------|
| Eval 数据集 | `GET/POST /eval/datasets`、cases、run、runs 查询 |
| Learning 建议 | `POST /learning/suggest-cases`（execution / langfuse 等多源） |
| Learning Cycle | `POST /learning/run-cycle`、`GET /learning/cycles` |

### 4.8 Zest Stack / M5 门户

| 能力 | 端点 |
|------|------|
| 能力栈 | `GET /capability-stack`、`/tiers/{tierId}`、`/export?tier=` |
| 场景模板 | `GET /scenario-templates`、`POST .../apply` |
| AI Jobs | `GET /ai-jobs/overview`、`POST /ai-jobs/wizard`（**草稿幂等** `v-tpl-{slug}`） |
| MCP 服务 | MCP Server CRUD |
| Flow 链 | `GET /flow-chains` |
| Playground | `POST /playground/preview`、invoke 试跑 |
| 适配器健康 | `GET /adapters/health`、`/health/all` |

---

## 5. Runtime API（业务 / Agent 调用）

前缀：`/v1`（App Token / OIDC JWT）。

| 端点 | 说明 |
|------|------|
| `POST /v1/llm/prepare` | 获取 traceId、promptVersion、routePolicy、profileVersion |
| `POST /v1/llm/invoke` | 同步推理（经 LiteLLM / Profile 策略） |
| `POST /v1/llm/invoke/stream` | SSE 流式 |
| `POST /v1/llm/report` | 异步上报 token/cost/output |
| `GET /v1/executions/{traceId}` | Runtime 侧 execution 查询 |

**Profile runtimeMode**：`invoke` | `agent` | `external` | `hybrid`。

---

## 6. 业务接入

### 6.1 `@ZestLLM` Starter

```java
@ZestLLM(code = "aiChat", timeoutMs = 30000, retry = 1)
public AiChatResult aiChat(@AiInput("question") String question, @AiOutput AiChatResult result) { ... }
```

- 支持 **invoke** 与 **agent** 模式（`zest.llm.runtime-mode`）。  
- Agent 本地缓存 Policy；变更后 Admin 侧 cache invalidate。

### 6.2 Demo（order-service）

| 能力 | 说明 |
|------|------|
| methodA / methodB | 同步 AI + traceId |
| flowChat | ZestFlow 双 Executor 示例 |
| aiChatTools | Tool Loop + MCP mock |

默认：`appKey=order-service`，`auth-token=demo-token-123`。

### 6.3 ZestFlow

| 组件 | 端口 | 说明 |
|------|------|------|
| Control Plane | 20552 | `POST /api/execute`，链如 `CHN_ZESTLLM_AI_CHAT` |
| Flow Gateway | Admin 内 | `/flow/ai-chat`、`tool-loop` 等 |
| Flow 适配器 | — | `ZEST_LLM` 节点执行器 |

---

## 7. 内置场景模板

| ID | 名称 | 推荐 Tier | runtimeMode | 要点 |
|----|------|-----------|-------------|------|
| `chat-basic` | 智能对话 | small | agent | native + LiteLLM |
| `report-basic` | 报表解读 | medium | hybrid | knowledge + learningLoop |
| `knowledge-qa` | 知识问答 | medium | agent | dify-kb + learningLoop |
| `ops-monitor` | 运维诊断 | large | agent | MCP + Dify + RAG 引用 |

Wizard 应用模板 → 创建/更新 Task + Profile 草稿（版本 `v-tpl-{slug}`，重复应用更新 DRAFT）。

---

## 8. Zest Stack 分层部署

| Tier | 命令 | 典型组件 |
|------|------|----------|
| **small** | `zest-stack-up.ps1 -Tier small` | MySQL + LiteLLM + mock + Admin + Demo + MCP |
| **medium** | `-Tier medium` | + Valkey + Langfuse |
| **large** | `-Tier large` | + Kafka report + Dify/RAGFlow integration profile |

详见 [Zest-Stack完整实现方案.md](./Zest-Stack完整实现方案.md)。

---

## 9. SPI 适配器清单

通过 `zest.llm.adapters.*` 配置切换，**零业务代码变更**。

| SPI | 实现 | 配置示例 |
|-----|------|----------|
| ModelGateway | LiteLLMGatewayAdapter | `litellm` |
| AgentRuntime | Native / Dify / Noop | `native` / `dify` |
| Observability | Langfuse / Noop | `langfuse` / `noop` |
| PolicyCache | Caffeine / Valkey | `caffeine` / `valkey` |
| ResponseCache | Valkey / Noop | `valkey` / `noop` |
| Quota | DB / Redis Token Bucket | `db` |
| Knowledge | RAGFlow / Noop | `ragflow` / `noop` |
| LearningPipeline | ZestEval / Noop | `zest-eval` / `noop` |
| ReportChannel | Sync / Kafka | `sync` / `kafka` |
| AlertWebhook | HTTP / Noop | `http` / `noop` |
| McpTool | HttpMcpToolAdapter | Profile tools |
| ContentModeration | KeywordBlocklist / Noop | `keyword-blocklist` |
| PromptRenderer | Handlebars | `handlebars` |
| OutputSchema | JSON Schema | `json` |
| SecretResolver | Env + Vault + Composite | `env:` / `vault:` |

---

## 10. 可靠性与运维机制

| 机制 | 说明 |
|------|------|
| 发布门禁 | Eval 通过率 + Probe 通过才允许 publish |
| Probe 巡检 | 手动 run-all + 可配置定时任务 |
| 执行审计 | `llm_execution` 全链路 traceId |
| Execution 归档 | 热表保留 + 冷归档表 + Admin 统计 |
| 失败样本闭环 | Execution → Learning suggest-cases → Eval 扩充 |
| 降级 | Profile `model.fallback` 链 |
| 超时 | `generation.timeoutMs` + 网关超时 |
| FinOps | 日成本阈值 + Webhook + 告警落库 |

---

## 11. 本地完整版工具链

### 11.1 启动

```powershell
# Windows（推荐单端口）
powershell -File deploy/scripts/start-local-full.ps1 -EmbedUi
# 可选：-WithLiteLLM 拉起 LiteLLM + openai-mock
```

```bash
# macOS / Linux
bash deploy/scripts/start-local-full.sh --embed-ui
# 可选：--with-litellm
```

| 入口 | 地址 |
|------|------|
| Admin（嵌入 UI） | http://127.0.0.1:8088 |
| Admin UI dev | http://localhost:5174 |
| 构建信息 | `GET /api/admin/meta/build` |

进程 PID：`deploy/logs/pids/*.pid`（停止脚本不误杀同端口其他进程）。

### 11.2 验证

```powershell
powershell -File deploy/scripts/verify-local.ps1   # mvn test + full-acceptance
```

```bash
bash deploy/scripts/verify-local.sh
```

### 11.3 其他脚本

| 脚本 | 用途 |
|------|------|
| `build-admin-ui.ps1` / `.sh` | UI 构建并嵌入 `admin/static` |
| `full-acceptance.ps1` | 66+ 项 Admin API 验收（含 SSO/CHAIN/REGISTRY、AC39–53 Wizard） |
| `sso-smoke.ps1` / `sso-smoke.sh` | Admin SSO Discovery / config / authorize 冒烟 |
| `production-acceptance.ps1` / `.sh` | 五阶段生产验收（白盒→黑盒→SSO→链路→压测） |
| `e2e-acceptance.sh` | Docker 栈 E2E（AC1–53） |
| `run-journeys.sh` | MCP/CI 旅程 |
| `stress-test-prepare.ps1` | prepare 压测 P50/P95 |
| `loadtest-cp-prepare.sh` | CP prepare 负载测试 |
| `start-plan-a.sh` | Langfuse 方案 A 全栈 |
| `zest-stack-up.ps1` / `.sh` | 分层一键 Compose |
| `gitops-sync-profiles.sh` | Profile GitOps 同步示例 |

---

## 12. Docker / Helm / CI

### 12.1 Docker Compose

- 主文件：`deploy/docker-compose.yml`  
- 扩展：`docker-compose.plan-a.yml`（Langfuse）、`docker-compose.kafka.yml`、`docker-compose.integration.yml`  
- 镜像：Admin、Demo、LiteLLM、MySQL、Valkey、mock 服务等  

### 12.2 Helm Chart

路径：`deploy/helm/zest-llm/`

| 资源 | 说明 |
|------|------|
| Deployment | Admin、Demo（可选）、LiteLLM（可选） |
| Service | Admin / Demo / LiteLLM |
| Secret | MySQL 密码（或 `existingSecret`） |
| Ingress | 可选，Admin + Demo `/demo` |
| HPA | Admin 可选水平扩缩 |
| Probes | Admin/Demo 存活与就绪探针 |
| Admin env | JWT + SSO（`ZEST_LLM_ADMIN_*`，Secret 注入）；SSO 多副本须 Redis |

### 12.3 CI 门禁（`.github/workflows/zestflow-acceptance.yml`）

| Job | 内容 |
|-----|------|
| build-and-test | `mvn test` + UI build embed + **static 目录 diff 门禁** |
| docker-e2e | Compose 全栈 + `e2e-acceptance.sh` + `run-journeys.sh` |

> 若仓库仅在 Gitee 托管、未镜像 GitHub，见 [Gitee-CI与生产签字.md](./Gitee-CI与生产签字.md)。

---

## 13. 验收标准（AC1–AC53）摘要

| 范围 | 编号 | 验证方式 |
|------|------|----------|
| Demo / 鉴权 / Prompt 热更新 | AC1–AC6 | E2E + Demo |
| Profile / Provider / OIDC 配置 | AC7–AC10 | E2E |
| MCP / Stream / ZestFlow / Playground / Eval | AC11–AC23 | E2E |
| 缓存 / Flow 链 / FinOps / 归档 | AC24–AC28 | E2E |
| Agent Probe / Dashboard / Ops | AC29–AC38 | E2E + full-acceptance |
| 整合 extensions / Zest Stack | AC39–AC48 | full-acceptance |
| M5：预览 / 总览 / 导出 / Wizard / Learning | AC49–AC53 | E2E + full-acceptance |

完整条目：[产品验收标准.md](./产品验收标准.md)。

**本地完整版达标线**：`production-acceptance.ps1` 五阶段全绿（含 **66+ PASS** 黑盒 + CHAIN/REGISTRY/SSO）。

**生产签字达标线**：`production-acceptance.sh`（Docker）五阶段 + 签字表 GATE-* 全 PASS（含 **GATE-SSO**）。

---

## 14. 完整版 vs 下一阶段对照

| 能力 | 完整版状态 | 下一阶段 |
|------|------------|----------|
| 控制面 API + UI | ✅ 齐备 | UI  polish、国际化 |
| 本地一键启动/验收 | ✅ 66+ PASS | Gitee CI docker-e2e manual |
| Docker small/medium E2E | ✅ CI 覆盖 | Large tier 专项验收 |
| Helm 最小可部署 | ✅ Chart + **SSO/JWT env** + 生产 values | 多 AZ、ExternalSecrets |
| Wizard / 场景模板 | ✅ 幂等草稿 + **`knowledge-qa` 内置模板** | 更多行业模板 |
| Learning 闭环 | ✅ API + UI + 定时 Job + **可选自动 publish**（`enabled` 默认 false；`audit-only` 人工复核） | 生产启用前审批流 |
| 知识检索适配器 | ✅ `ragflow` + **`dify-kb`** + `noop` | Large tier 联调验收 |
| Admin SSO 登录页 | ✅ 可插拔 SPI + **Back-Channel Logout 会话吊销** | 生产 IdP 联调见 [ZestLLM-Admin-SSO.md](./ZestLLM-Admin-SSO.md) |
| 生产签字 / SLO | ❌ 未做 | 压测报告 + 变更流程 |

---

## 15. 相关文档

| 文档 | 内容 |
|------|------|
| [README.md](../README.md) | 快速开始、模块、本地完整版 |
| [完整版推进状态.md](./完整版推进状态.md) | 模块状态、Linux 测试机下一步 |
| [ZestLLM-Admin-SSO.md](./ZestLLM-Admin-SSO.md) | Admin SSO 配置与联调 |
| [Gitee-CI与生产签字.md](./Gitee-CI与生产签字.md) | Gitee CI 与生产签字流程 |
| [生产级全量测试计划.md](./生产级全量测试计划.md) | 五阶段全量测试计划 |
| [Zest-Stack完整实现方案.md](./Zest-Stack完整实现方案.md) | 分层部署与架构 |
| [产品验收标准.md](./产品验收标准.md) | AC 门禁 |
| [Agent配置模型.md](./Agent配置模型.md) | Profile SSOT |
| [ZestLLM-立项交接.md](./ZestLLM-立项交接.md) | 立项背景与边界 |
| [deploy/helm/zest-llm/README.md](../deploy/helm/zest-llm/README.md) | Helm 安装 |

---

*本文档随代码演进更新；以 `GET /api/admin/meta/features` 与 `GET /api/admin/meta/build` 为准核对运行实例版本。*
