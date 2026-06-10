# ZestLLM

AI 作业调度与治理平台（LLM Control Plane）。业务通过 `@ZestLLM` 注解声明 AI 作业，平台统一执行 Prompt 匹配、模型路由、结果回填与治理。

**数据库：仅 MySQL 8**（禁止 H2 / PostgreSQL）。

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

```bash
cd deploy
docker compose up -d mysql    # 先起 MySQL
docker compose up -d --build  # 全栈（含 openai-mock，无需真实 API Key）
```

**方案 A 生产推荐**（Langfuse 可观测 + Admin observability profile）：

```bash
bash deploy/scripts/start-plan-a.sh
bash deploy/scripts/e2e-acceptance.sh   # AC1–AC28
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
```

完整标准见 [docs/产品验收标准.md](docs/产品验收标准.md)。

## 本地开发

```bash
cd deploy && docker compose up -d mysql

cp zest-llm-admin/src/main/resources/application-local.example.yml \
   zest-llm-admin/src/main/resources/application-local.yml

mvn -pl zest-llm-admin -am spring-boot:run -Dspring-boot.run.profiles=local
cd zest-llm-admin-ui && npm run dev   # http://localhost:5174
```

JDBC 连接串见 `application-local.yml`（由 example 复制后按本机 MySQL 密码修改，**勿**在 `application.yml` 写死账号密码）。

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

## 产品验收标准（AC1–AC28）

| # | 用例 | 验证方式 |
|---|------|----------|
| AC1 | Demo 同步 AI 返回 answer + traceId | `GET /demo/order/methodA?orderId=1&question=hello` |
| AC2 | Admin 按 traceId 查 Execution | Admin UI → 执行记录 |
| AC3 | Prompt 发布后立即生效（缓存失效） | Admin 发布 v2，不重启 Demo |
| AC4 | SPI noop 下 invoke 仍成功 | `observability: noop` |
| AC5 | MCP/CI 验收旅程 | `deploy/scripts/run-journeys.sh` |
| AC6 | 错误 appToken → AUTH_FAILED | Runtime 返回 FAILED |
| AC7–AC10 | Profile / Provider / OIDC 配置 | `e2e-acceptance.sh` |
| AC11–AC15 | MCP CRUD / stream SSE | `e2e-acceptance.sh` |
| AC16–AC17 | ZestFlow 真实 DAG | `:20552/api/execute` |
| AC18–AC19 | Tool Loop Profile / Flow Gateway | `e2e-acceptance.sh` |
| AC20 | Demo 双 Executor flowChat | `GET /demo/order/flowChat` |
| AC21 | Tool Loop 真调用链 | invoke `aiChatTools` 含 `tool-loop-ok` |
| AC22 | Prompt Playground 预览 | Admin `/api/admin/playground/preview` |
| AC23 | Eval 批量评测 | Admin `/api/admin/eval/datasets/demo-aichat/run` |
| AC24 | 语义响应缓存 | 同 prompt 二次 invoke `metrics.cacheHit=true` |
| AC25 | ZestFlow 链 DB 注册表 | Admin `/api/admin/flow-chains` |
| AC26 | FinOps 成本告警 Webhook | 配额含 `alertWebhookUrl` |
| AC27 | Eval 数据集创建 | Admin POST `/api/admin/eval/datasets` |
| AC28 | Execution 归档统计 | Admin `/api/admin/executions/archive/stats` |

Phase3 额外能力：FinOps 成本告警、Kafka Report profile、Flow 链 Admin UI、Eval CRUD、Execution 归档默认开启（Docker）。

自动化：`bash deploy/scripts/e2e-acceptance.sh` + `mvn test`

## API 文档

- Swagger：<http://localhost:8088/swagger-ui.html>

## 许可证

Apache License 2.0
