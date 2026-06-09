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
docker compose up -d --build    # 全栈
```

| 服务 | 端口 | 账号 |
|------|------|------|
| MySQL | 3306 | root / root，库 `zest_llm` |
| Admin + UI | 8088 | admin / admin123 |
| Demo | 8081 | appKey `order-service` / token `demo-token-123` |
| LiteLLM | 4000 | — |
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

## 产品验收标准（AC1–AC6）

| # | 用例 | 验证方式 |
|---|------|----------|
| AC1 | Demo 同步 AI 返回 answer + traceId | `GET /demo/order/methodA?orderId=1&question=hello` |
| AC2 | Admin 按 traceId 查 Execution | Admin UI → 执行记录 |
| AC3 | Prompt 发布后立即生效（缓存失效） | Admin 发布 v2，不重启 Demo，下次调用用 v2 |
| AC4 | SPI noop 下 invoke 仍成功 | `zest.llm.adapters.observability: noop` |
| AC5 | MCP/CI 验收旅程 | `.zestflow/acceptance/journeys.yml` |
| AC6 | 错误 appToken → AUTH_FAILED | Runtime 返回 `InvokeResponse.status=FAILED` |

自动化测试：`mvn -pl zest-llm-admin test`（鉴权、缓存失效、Runtime 错误契约）。

## API 文档

- Swagger：<http://localhost:8088/swagger-ui.html>

## 许可证

Apache License 2.0
