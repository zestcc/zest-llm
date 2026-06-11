# LiteLLM 完全入门：统一接入 OpenAI / DeepSeek / Ollama 的模型网关（附 ZestLLM 案例）

> **CSDN 发布用短文版（约 3000 字）** · 完整版见 [LiteLLM从入门到实践.md](./LiteLLM从入门到实践.md)

---

## 前言

接大模型 API 的人多半经历过这样的路径：先用 OpenAI SDK 写个 Demo，再加 DeepSeek 降本，再试 Ollama 本地——每加一个模型，就多一套协议、多一套 Key、多一段 fallback 逻辑。

**LiteLLM** 要解决的，就是把这一团乱麻收成 **一个 HTTP 网关、一种 API**。本文以学习 LiteLLM 为核心，文末附 ZestLLM 的真实接入案例。

---

## 一、LiteLLM 是什么？

**LiteLLM = 部署在你机器上的大模型统一接入网关。**

- 对外：OpenAI 兼容 API（`POST /v1/chat/completions` 等），默认端口 **4000**
- 对内：按配置转发到 OpenAI、DeepSeek、Claude、Ollama 等 100+ 上游

它**不是**大模型本身，**不是** ChatGPT 替代品，**主要是 Proxy 服务**（而不只是 Python SDK）。

```text
App A ──┐
App B ──┼──► LiteLLM :4000 ──► OpenAI / DeepSeek / Ollama
App C ──┘
```

---

## 二、三个必会概念

### 1. model_list — 模型注册表

```yaml
model_list:
  - model_name: gpt-4o-mini       # 客户端传的 model
    litellm_params:
      model: openai/gpt-4o-mini   # 实际上游
      api_key: os.environ/OPENAI_API_KEY
```

### 2. master_key — 网关鉴权

```yaml
general_settings:
  master_key: sk-demo-key
```

调用 LiteLLM 时：`Authorization: Bearer sk-demo-key`

### 3. 两层 Key（必理解）

```text
你的应用 ──[master_key]──► LiteLLM ──[OPENAI_API_KEY]──► OpenAI
```

- **master_key**：谁能调你的网关
- **上游 Key**：网关帮你调厂商

---

## 三、5 分钟跑通

```bash
pip install "litellm[proxy]"

# config.yaml
# model_list + master_key + OPENAI_API_KEY 环境变量

litellm --config config.yaml --port 4000
```

健康检查：

```bash
curl http://localhost:4000/health/liveliness
```

第一次对话：

```bash
curl http://localhost:4000/v1/chat/completions \
  -H "Authorization: Bearer sk-demo-key" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}'
```

**排错速查**：

| 报错 | 原因 |
|------|------|
| No api key passed in | 没带 master_key |
| Missing credentials | 没设 OPENAI_API_KEY |
| model not found | model 名与 model_name 不一致 |

---

## 四、进阶要点（知道即可，用到再深学）

| 能力 | 一句话 |
|------|--------|
| 多模型 | 改 `model` 字段，不动代码 |
| 流式 | `"stream": true`，SSE 格式同 OpenAI |
| Ollama 本地 | `model: ollama/llama3` + `api_base: http://localhost:11434` |
| Fallback | 网关路由组 或 业务层 primary→fallback 链 |
| 生产 | 独立部署 Sidecar，Key 不进 Git，镜像用 `-stable` tag |

**和 OneAPI / OpenRouter 怎么选？**

- 自托管、企业内网、多服务共用 → **LiteLLM / OneAPI**
- 不想运维、快速试模型 → **OpenRouter**
- 单 App 单模型 POC → 可先直连，规模化再上网关

---

## 五、网关 vs 控制面（建立架构观）

| | LiteLLM（网关） | ZestLLM（控制面） |
|---|----------------|-------------------|
| 干什么 | 连模型、路由、限流 | Prompt、发布、审计、Eval |
| 流量 | 重（token） | 轻（JSON 策略） |

两者**互补**：LiteLLM 不会帮你管 Prompt 版本；控制面也不该转发 token 流。

---

## 六、附：ZestLLM 接入 LiteLLM 案例

[ZestLLM](https://github.com/your-org/zestLLM) 是 Java 生态的 AI 作业治理平台。LiteLLM 作其**默认模型网关**，业务用 `@ZestLLM(code)` 声明作业，推理直连 LiteLLM。

### 调用链

```text
prepare  → Admin :8088（Prompt + 模型策略 + traceId）
execute  → LiteLLM :4000（实际推理）
report   → Admin（token / cost 审计）
```

### LiteLLM 配置（项目自带）

`deploy/litellm/config.yaml`：`gpt-4o-mini`、`deepseek-chat`，`master_key: sk-zest-llm-demo`

```powershell
powershell -File deploy/scripts/start-litellm-local.ps1
```

### ZestLLM 配置要点

```yaml
# Admin
zest.llm.litellm.base-url: http://localhost:4000
zest.llm.litellm.api-key: sk-zest-llm-demo

# Profile
model.primary: gpt-4o-mini
model.fallback: [deepseek-chat]
providers.litellm-default.baseUrl: http://localhost:4000
```

### 业务代码

```java
@ZestLLM(code = "aiChat")
public AiChatResult aiChat(@AiInput("question") String q, @AiOutput AiChatResult r) {
    return r;  // 换模型只改 Admin Profile，不改代码
}
```

### 验证

```bash
curl -H "Authorization: Bearer demo-token-123" \
     -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hello"}}' \
     http://localhost:8088/v1/llm/invoke
```

---

## 七、总结

1. **LiteLLM 是模型接入层**，把 N 个厂商收成一种 OpenAI API  
2. **必会**：`model_list`、`master_key`、两层 Key、curl 调通  
3. **进阶**：流式、Fallback、Sidecar 独立部署  
4. **案例**：ZestLLM 把 LiteLLM 放在 execute 层，控制面只管治理  

**建议学习顺序**：pip 跑通 → 接第二个模型 → 理解 Key 分层 → 看 ZestLLM 案例。

---

> **标签**：LiteLLM · 模型网关 · OpenAI Compatible · 大模型接入 · ZestLLM · Java AI  
> **完整版**：[docs/LiteLLM从入门到实践.md](./LiteLLM从入门到实践.md)
