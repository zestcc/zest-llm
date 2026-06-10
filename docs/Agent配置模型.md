# ZestLLM 智能体配置模型

> 版本 1.0.0 · 对标 CC Switch / LiteLLM Router / OpenAI Assistants 的配置分层思路

## 1. 设计目标

- **单一真相源（SSOT）**：`llm_agent_profile` 发布版 + Provider 预设
- **多入口**：Admin UI（表单+JSON）、REST import/export、Starter `profile-ref` 本地覆盖
- **多 Auth**：Runtime 入站（STATIC_TOKEN / OIDC_JWT / API_KEY）与 Upstream 出站（API_KEY_REF）分离
- **一键切换 Provider**：无需改业务代码，发布/激活后 Policy Cache 失效

## 2. 配置分层

```text
Global Provider Preset  →  Tenant（可选）→  App Auth Binding  →  Task Agent Profile  →  Environment override
```

| 存储 | 表 / 配置 | 说明 |
|------|-----------|------|
| Provider 预设 | `llm_provider_preset` | LiteLLM / OpenRouter 等网关模板 |
| Auth 绑定 | `llm_auth_binding` + `llm_app.auth_*` | 入站/出站鉴权 |
| Agent Profile | `llm_agent_profile` | 版本化 JSON，可发布/回滚 |
| 兼容层 | Prompt + ModelRoute + `policy_json` | 无 Profile 时自动合成 |

## 3. Profile JSON Schema（zestllm/v1）

```json
{
  "apiVersion": "zestllm/v1",
  "runtimeMode": "agent",
  "providerRef": "litellm-default",
  "model": {
    "primary": "gpt-4o-mini",
    "fallback": ["gpt-3.5-turbo"]
  },
  "generation": {
    "maxTokens": 1024,
    "temperature": 0.7,
    "timeoutMs": 30000
  },
  "tools": [
    { "type": "mcp", "name": "docs", "serverRef": "internal-docs" }
  ],
  "guardrails": {
    "piiRedact": false,
    "blockOnSchemaMismatch": true
  },
  "providers": {
    "litellm-default": {
      "type": "litellm",
      "baseUrl": "http://litellm:4000",
      "protocol": "openai"
    }
  },
  "inboundAuth": { "mode": "STATIC_TOKEN" },
  "outboundAuth": { "mode": "API_KEY_REF", "secretRef": "env:LITELLM_API_KEY" }
}
```

## 4. Runtime 下发（PrepareResponse）

prepare 阶段向 Agent/invoke 下发 **不含密钥** 的完整策略：

- `profileVersion` / `runtimeMode` / `providerRef`
- `gatewayBaseUrl` / `gatewayProtocol`
- `providers`（脱敏）
- `tools` / `guardrails`
- 原有 `renderedPrompt` / `model` / `fallbackModels` 等

## 5. Auth 模式

| 模式 | 场景 | 配置示例 |
|------|------|----------|
| STATIC_TOKEN | 默认，业务 Bearer Token | `{"mode":"STATIC_TOKEN"}` |
| OIDC_JWT | 企业 IdP JWT | `{"mode":"OIDC_JWT","issuer":"https://idp/","audience":"zest-llm","jwksUri":"..."}` |
| API_KEY | 固定 API Key 哈希 | `{"mode":"API_KEY","extra":{"apiKeyHash":"..."}}` |
| API_KEY_REF | 出站 LiteLLM | `{"mode":"API_KEY_REF","secretRef":"env:LITELLM_API_KEY"}` |

## 6. Admin API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/agent-profiles/{taskCode}/versions` | 列出版本 |
| POST | `/api/admin/agent-profiles/{taskCode}/versions` | 新建草稿 |
| POST | `/api/admin/agent-profiles/{taskCode}/publish` | 发布 |
| POST | `/api/admin/agent-profiles/import` | JSON 导入 |
| POST | `/api/admin/agent-profiles/{taskCode}/activate-provider` | 一键切换 Provider |
| GET | `/api/admin/provider-presets` | Provider 预设列表 |
| PUT | `/api/admin/auth-bindings` | Auth 绑定 |

## 7. Starter 本地覆盖

```yaml
zest:
  llm:
    app-key: order-service
    auth-token: ${ZEST_LLM_APP_TOKEN}
    profile-ref: aiChat@v1
    runtime-mode: agent
    overrides:
      providers.litellm-default.baseUrl: http://localhost:4000
```

## 8. 参考方案

- [CC Switch](https://github.com/farion1231/cc-switch)：多 Provider 预设、JSON 导入导出、OAuth MCP
- [LiteLLM Router](https://docs.litellm.ai/)：模型 fallback 与网关抽象
- [OpenAI Assistants API](https://platform.openai.com/docs/assistants/overview)：tools + guardrails 分离

## 9. SecretRef 解析（Agent 侧）

| 前缀 | 示例 | 说明 |
|------|------|------|
| `env:` | `env:LITELLM_API_KEY` | 环境变量 / System Property |
| `vault:` | `vault:llm/litellm#apiKey` | HashiCorp Vault KV v2 |

配置：

```yaml
zest:
  llm:
    vault:
      address: http://127.0.0.1:8200
      token: ${VAULT_TOKEN}
      mount: secret
```

`PrepareResponse.outboundSecretRef` 仅下发引用，**密钥不在 CP 明文传输**。

## 10. MCP Tools 执行链

1. Profile 声明 `tools: [{type:mcp, serverRef:internal-docs, config:{toolName:search}}]`
2. CP 从 `llm_mcp_server` 解析 `serverUrl` / `authSecretRef`
3. Agent `AgentToolOrchestrator` 调用 `McpToolAdapter`（JSON-RPC `tools/call`）
4. 工具结果注入 Prompt 后直连 LiteLLM

Admin API：`GET /api/admin/mcp-servers`
