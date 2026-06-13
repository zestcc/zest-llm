# ZestLLM

**XXL-Job for AI** — Java AI 控制面：A 层选能力栈，B 层配场景，业务 `@ZestLLM(code)` 定作业，平台统一调度 Prompt / 模型 / 治理。

AI 作业调度与治理平台（LLM Control Plane）。业务通过 `@ZestLLM` 注解声明 AI 作业，平台统一执行 Prompt 匹配、模型路由、结果回填与治理。

**数据库：仅 MySQL 8**（禁止 H2 / PostgreSQL）。

完整方案见 [docs/Zest-Stack完整实现方案.md](docs/Zest-Stack完整实现方案.md)。  
**完整版功能清单**见 [docs/ZestLLM-完整版功能清单.md](docs/ZestLLM-完整版功能清单.md)。

## 模块结构

| 模块 | 说明 |
|------|------|
| `zest-llm-spi` | 可插拔适配器 SPI（Gateway / Observability / Quota / Cache / Schema / Audit） |
| `zest-llm-common` | 公共 API、错误码、prepare/report 契约 |
| `zest-llm-infra` | LiteLLM / Handlebars / Caffeine / JSON Schema 默认实现 |
| `zest-llm-admin` | Control Plane + Admin API + 静态 UI |
| `zest-llm-admin-ui` | Vue3 Admin 前端 |
| `zest-llm-starter` | `@ZestLLM` 注解 + AOP（invoke / agent 双模式） |
| `zest-llm-agent` | 业务侧 prepare → execute(LiteLLM) → report |
| `zest-llm-flow-adapter` | ZestFlow `ZEST_LLM` 节点执行器 |
| `zest-llm-demo` | 业务接入示例 |

## 构建

```bash
mvn clean install
cd zest-llm-admin-ui && npm install && npm run build
```

## 一键 Docker 部署

### Zest Stack 分层（推荐）

| Tier | 场景 | 命令 |
|------|------|------|
| **small** | POC / 1～2 作业 | `powershell -File deploy/scripts/zest-stack-up.ps1 -Tier small` |
| **medium** | 部门级 + Langfuse | `-Tier medium` |
| **large** | Dify + RAGFlow + Kafka | `-Tier large` |

```bash
cd deploy
docker compose up -d mysql    # 先起 MySQL
docker compose up -d --build  # 全栈（含 openai-mock，无需真实 API Key）
```

**方案 A 生产推荐**（Langfuse 可观测 + Admin observability profile）：

```bash
bash deploy/scripts/start-plan-a.sh
bash deploy/scripts/e2e-acceptance.sh   # AC1–AC36
bash deploy/scripts/loadtest-cp-prepare.sh
```

| 服务 | 端口 | 账号 |
|------|------|------|
| MySQL | 3306 | root / root，库 `zest_llm` |
| Admin + UI | 8088 | admin / admin123 |
| Demo | 8081 | appKey `order-service` / token `demo-token-123` |
| LiteLLM | 4000 | 上游 openai-mock |
| Langfuse（方案 A） | 3000 | pk-lf-zest-demo / sk-lf-zest-demo |
| ZestFlow CP | 20552 | POST `/api/execute` |
| ZestFlow Demo | 20551 | POST `/api/execute` |
| Valkey | 6379 | — |

验收脚本：

```bash
bash deploy/scripts/e2e-acceptance.sh
powershell -File deploy/scripts/full-acceptance.ps1   # 含 AC39-48
powershell -File deploy/scripts/stress-test-prepare.ps1 -Concurrency 50 -Total 200
```

完整标准见 [docs/产品验收标准.md](docs/产品验收标准.md)。

## 本地开发（完整版）

**推荐单端口（演示/联调）**：`-EmbedUi` 将 UI 嵌入 `:8088`，无需 Vite dev server。

**LiteLLM**：未加 `-WithLiteLLM` / `--with-litellm` 时 Dashboard 显示 Gateway DOWN 属预期。

- **有 Docker**：`-WithLiteLLM` 拉起 compose 中的 mock 网关。
- **无 Docker**：先 `pip install "litellm[proxy]"`，再 `-WithLiteLLM`（自动走 `start-litellm-local.ps1` + `config-local.yaml` mock 模型，无需 API Key）。

```powershell
pip install "litellm[proxy]"
powershell -File deploy/scripts/start-litellm-local.ps1          # 仅 LiteLLM mock
powershell -File deploy/scripts/start-litellm-local.ps1 -RealModels  # 真实 config.yaml
```

```powershell
# Windows
Copy-Item zest-llm-admin/src/main/resources/application-local.example.yml `
          zest-llm-admin/src/main/resources/application-local.yml
powershell -File deploy/scripts/start-local-full.ps1 -EmbedUi    # 或 -WithLiteLLM
powershell -File deploy/scripts/verify-local.ps1
powershell -File deploy/scripts/start-local-full.ps1 -StopOnly
```

```bash
# macOS / Linux
cp zest-llm-admin/src/main/resources/application-local.example.yml \
   zest-llm-admin/src/main/resources/application-local.yml
bash deploy/scripts/start-local-full.sh --embed-ui   # 或 --with-litellm
bash deploy/scripts/verify-local.sh
bash deploy/scripts/start-local-full.sh --stop-only
```

| 入口 | 地址 |
|------|------|
| Admin（嵌入 UI，推荐） | http://127.0.0.1:8088 |
| Admin UI dev（热更新） | http://localhost:5174 |
| 构建信息 API | `GET /api/admin/meta/build`（含 gitCommit / buildTime） |
| 登录 | admin / admin123 |

进程通过 `deploy/logs/pids/*.pid` 管理，停止脚本不会误杀同端口其他服务。

## 业务接入

```yaml
zest:
  llm:
    enabled: true
    runtime-mode: agent
    control-plane-url: http://127.0.0.1:8088
    app-key: order-service
    auth-token: demo-token-123
    agent:
      enabled: true
      litellm-url: http://127.0.0.1:4000
```

## 产品验收标准（AC1–AC53）

| # | 用例 | 验证方式 |
|---|------|----------|
| AC1 | Demo 同步 AI 返回 answer + traceId | `GET /demo/order/methodA?orderId=1&question=hello` |
| AC2 | Admin 按 traceId 查 Execution | Admin UI → 执行记录 |
| AC3 | Prompt 发布后立即生效（缓存失效） | Admin 发布 v2，不重启 Demo |
| AC4 | SPI noop 下 invoke 仍成功 | `observability: noop` |
| AC5 | MCP/CI 验收旅程 | `deploy/scripts/run-journeys.sh` |
| AC6 | 错误 appToken → AUTH_FAILED | Runtime 返回 FAILED |
| AC7–AC10 | Profile / Provider / OIDC 配置 | `e2e-acceptance.sh` |
| AC11–AC36 | MCP / ZestFlow / Eval / Probe / Ops 等 | `e2e-acceptance.sh` |
| AC39–AC48 | 整合扩展 / Zest Stack / 场景模板 | `full-acceptance.ps1` |
| AC49–AC53 | 发布预览 / Wizard / Learning 等 M5 | `e2e-acceptance.sh` |

运维手册：[docs/智能体探测与运维手册.md](docs/智能体探测与运维手册.md)

Phase3 额外能力：FinOps 成本告警、Kafka Report profile、Flow 链 Admin UI、Eval CRUD、Execution 归档默认开启（Docker）。

自动化：`bash deploy/scripts/e2e-acceptance.sh` + `mvn test`

## API 文档

- Swagger：<http://localhost:8088/swagger-ui.html>

## 许可证

Apache License 2.0
