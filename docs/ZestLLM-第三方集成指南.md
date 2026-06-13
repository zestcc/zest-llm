# ZestLLM 第三方集成指南

> 目标：30–60 分钟完成首个 Agent 接入 · 不含 ZestStory 专用代码

## 1. 接入路径（推荐 45 分钟）

| 步骤 | 时间 | 动作 |
|------|------|------|
| 1 | 5 min | 启动 small 栈：`zest-stack-up.ps1 -Tier small` |
| 2 | 10 min | 确认 LiteLLM + Admin；`GET /api/admin/gateway-models` |
| 3 | 10 min | 导入 Provider Preset + Profile（Import API 或 Wizard） |
| 4 | 10 min | Demo / HTTP 调用 `POST /v1/llm/prepare` |
| 5 | 10 min | Probe + Publish Preview；可选发布 |

## 2. 两种集成方式

### 2.1 HTTP / REST（语言无关）

```bash
# prepare
curl -X POST http://localhost:8088/v1/llm/prepare \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hello"}}'
```

业务侧读取 `renderedPrompt` / `knowledgePrefetch`，自行调用 LiteLLM。

### 2.2 @ZestLLM Starter（Java）

```java
@ZestLLM(appKey = "order-service", code = "aiChat")
public ChatResult chat(@ZestInput("question") String question) { ... }
```

Starter 自动注册 Method、拉取 Policy、可选直连 LiteLLM（native runtime）。

## 3. Integration Import API

```bash
# 幂等导入 gateway model
curl -X POST http://localhost:8088/api/admin/integration/import/gateway-models \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"modelName":"deepseek-v4-flash","upstreamModel":"deepseek/deepseek-v4-flash","apiKeySecretRef":"deepseek-api-key"}]}'
```

示例 Profile 见 `examples/integration/generic-chat-agent/profile.yaml`。

## 4. 错误码（常见）

| 码 | HTTP | 含义 |
|----|------|------|
| TASK_NOT_FOUND | 400 | taskCode 不存在 |
| PROFILE_NOT_FOUND | 400 | Profile 版本不存在 |
| PROBE_FAILED | 409 | 发布/Probe 未通过 |
| EVAL_BELOW_THRESHOLD | 409 | Eval 通过率低于 learningLoop.minPassRate |
| MODEL_EXISTS | 400 | gateway model 重名 |
| SECRET_REF_NOT_FOUND | 400 | api_key_secret_ref 无效 |
| PRESET_EXISTS | 400 | provider preset 已存在（非 import 路径） |

## 5. Learning 自动发布（audit-only）

`LearningAutoPublishService` 在定时周期内评估已发布 Profile，**默认仅写审计日志**，不自动 publish。

- 配置：`zest-llm.admin.learning.auto-publish.enabled=false`（推荐生产保持 false）
- 启用自动发布前必须：`learningLoop.reviewRequired=false` 且明确运维签字
- 集成方应依赖 **Publish Preview**（含 `evalGateSummary`）+ 人工发布，而非依赖 auto-publish

## 6. Publish Webhook（可选）

```yaml
zest-llm.admin.integration.webhook-url: https://your-system/hooks/zestllm
```

Profile 发布成功/失败时 POST JSON：`event`, `taskCode`, `version`, `success`, `message`。

## 7. Tier 建议

| Tier | 适用 |
|------|------|
| small | POC、单模型、noop knowledge |
| medium | http-knowledge / Eval 门禁 |
| large | RAGFlow/Dify + Kafka report + LiteLLM sync |

详见 [ZestLLM-Integration-Suite.md](./ZestLLM-Integration-Suite.md)。
