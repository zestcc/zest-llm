# ZestLLM Integration Suite v1

> 版本 1.0.0 · 覆盖路线图 ①–④ · 生产级 small / medium / large 验收

## 1. 套件定义

Integration Suite 将第三方接入所需能力产品化为四个可交付模块：

| 模块 | 内容 | DoD |
|------|------|-----|
| **① SSOT + Sync + Import** | `llm_gateway_model` / `llm_secret_ref` 表；LiteLLM 同步；批量 Import API | Flyway V23；CRUD + sync；meta features 暴露 |
| **② 集成指南 + 示例** | 30–60 分钟接入路径；generic 示例 profile + scenario template | 文档 + `examples/integration/*` |
| **③ SPI 即插即用** | `http-knowledge` 适配器；tier 验收脚本 | 条件 Bean + health + acceptance.sh |
| **④ 治理产品化** | Publish Preview 扩展字段；可选 publish webhook | preview API + webhook hook |

## 2. Tier 矩阵

| 能力 | small | medium | large |
|------|-------|--------|-------|
| Gateway Model SSOT | ✓ | ✓ | ✓ |
| Secret Ref（脱敏列表） | ✓ | ✓ | ✓ |
| Integration Import API | ✓ | ✓ | ✓ |
| LiteLLM Sync（可选） | 手动 | 手动 | 全量 trigger |
| http-knowledge SPI | noop 可过 | 启用 + health | 启用 + 外部 KB |
| Publish Preview 扩展 | ✓ | ✓ + evalGate | ✓ + webhook |
| Generic 场景模板 | chat | hybrid-rag | agent-mcp |

## 3. Admin API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/gateway-models` | 模型 SSOT 列表 |
| POST | `/api/admin/gateway-models` | 注册模型 |
| PUT | `/api/admin/gateway-models/{modelName}` | 更新模型 |
| GET | `/api/admin/secret-refs` | 密钥引用（脱敏） |
| POST | `/api/admin/secret-refs` | 创建密钥引用 |
| POST | `/api/admin/integration/import/provider-presets` | 批量导入预设（幂等 presetCode） |
| POST | `/api/admin/integration/import/agent-profiles` | 批量导入 Profile |
| POST | `/api/admin/integration/import/gateway-models` | 批量导入模型（幂等 modelName） |
| POST | `/api/admin/integration/sync-litellm` | 触发 LiteLLM 全量同步 |
| GET | `/api/admin/integration/overview` | 集成健康概览（模型同步/密钥/适配器，参考 Portkey Dashboard） |
| POST | `/api/admin/integration/import/*` | 批量导入；`dryRun:true` 预览不写库（参考 Dify 导入预览） |
| GET | `/api/admin/integration/sync-litellm/status` | LiteLLM 同步状态（逐模型 syncStatus，参考 LiteLLM Proxy 模型列表） |
| GET | `/api/admin/integration/webhook/deliveries` | Publish Webhook 投递历史（分页；失败进 DLQ） |
| POST | `/api/admin/integration/webhook/deliveries/{id}/retry` | 手动重试失败/死信 Webhook |
| GET | `/api/admin/meta/features` | `integrationSuiteApi` 等 flags |

## 4. SPI 适配器矩阵

| 适配器 | 配置键 | small | medium | large |
|--------|--------|-------|--------|-------|
| knowledge-retrieval | `zest.llm.adapters.knowledge-retrieval` | `noop` | `http-knowledge` | `ragflow` / `dify-kb` |
| model-gateway | `zest.llm.adapters.model-gateway` | `litellm` | `litellm` | `litellm` |
| learning-pipeline | `zest.llm.adapters.learning-pipeline` | `noop` | `zest-eval` | `zest-eval` |

`http-knowledge` 配置：

```yaml
zest.llm.adapters.knowledge-retrieval: http-knowledge
zest.llm.http-knowledge:
  base-url: http://your-kb:8090
  retrieve-path: /v1/retrieve
  health-path: /health
```

## 5. 验收 AC57–67

| AC | 内容 | 脚本 |
|----|------|------|
| AC57 | gateway-models 列表含 seed 模型 | e2e / full-acceptance |
| AC58 | integration import 幂等 updated=1 | e2e |
| AC59 | publish-preview 含 adapterHealthSummary / knowledgeHealthUp | e2e |
| AC60 | generic-* 场景模板 | e2e |
| AC61 | secret-refs 列表 | e2e |
| AC62 | meta integrationSuiteApi | e2e |
| AC63 | integration overview API | e2e / full-acceptance |
| AC64 | import dry-run preview | e2e / full-acceptance |
| AC65 | LiteLLM sync status endpoint | e2e / full-acceptance |
| AC66 | agent-profile import dry-run would-update | e2e / full-acceptance |
| AC67 | webhook delivery history API | e2e / full-acceptance |

## 5.1 Learning 自动发布（默认关闭）

Eval 门禁未通过时 `LearningCycleResult.publishAllowed=false`，**不会**触发 auto-publish。

启用步骤（预发建议先 `audit-only: true`）：

```yaml
zest:
  llm:
    learning:
      auto-publish:
        enabled: true
        audit-only: false   # true=仅审计日志，不调用 publish
```

Admin UI **集成概览**（`/integration`）与 Publish Preview 的 `evalGateSummary` 可查看门禁详情。

## 6. Large Tier 侧车校验

不启动容器时校验 compose 合并配置：

```bash
bash deploy/scripts/validate-large-tier-compose.sh
```

启动：`bash deploy/scripts/zest-stack-up.sh large` · B 整合栈：`bash deploy/scripts/integration-demo.sh`

## 7. 相关文档

- [ZestLLM-第三方集成指南.md](./ZestLLM-第三方集成指南.md)
- [生产级全量测试计划.md](./生产级全量测试计划.md)
