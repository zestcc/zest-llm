# LiteLLM 从入门到实践：统一大模型接入的模型网关完全指南（附 ZestLLM 接入案例）

> **摘要**：LiteLLM 是一个开源的 LLM 代理网关，把 OpenAI、Claude、DeepSeek、通义、Ollama 等 100+ 模型统一成 **OpenAI Compatible API**。本文以**学习 LiteLLM** 为主线，讲清它是什么、解决什么问题、怎么用、和同类工具有何不同；文末附上 **ZestLLM 接入 LiteLLM** 的真实案例，帮助你在 Java 控制面场景里落地。
>
> **适合读者**：想系统学习 LiteLLM 的后端 / 架构 / 平台开发；文末案例面向已了解 Spring Boot 的 Java 开发者。
>
> **关键词**：LiteLLM · 模型网关 · OpenAI Compatible · LLM Proxy · 大模型接入 · ZestLLM
>
> **CSDN 建议标题**：LiteLLM 完全入门：统一接入 OpenAI / DeepSeek / Ollama 的模型网关（附 ZestLLM 案例）

---

## 一、LiteLLM 是什么？先建立正确认知

### 1.1 一句话定义

**LiteLLM = 部署在你基础设施上的「大模型统一接入网关」。**

对外：一个 HTTP 服务（默认 `:4000`），暴露与 OpenAI 几乎一致的 API，例如：

- `POST /v1/chat/completions`
- `POST /v1/embeddings`
- `GET  /health/liveliness`

对内：根据配置，把请求转发到 OpenAI、Anthropic、Azure、DeepSeek、Ollama 等上游，并完成协议转换、鉴权、路由、降级等。

### 1.2 它不是什么

| 误解 | 正解 |
|------|------|
| 又一个 Python SDK | 主要是 **Proxy Server（代理服务）**，SDK 只是调用方式之一 |
| 大模型本身 | 不训练、不推理，只做 **转发与治理** |
| ChatGPT 替代品 | 是 **基础设施**，上层才是你的业务 / 控制面 |
| 只能接 OpenAI | 通过 `model_list` 接大量厂商与本地模型 |

### 1.3 解决的核心问题

```text
没有 LiteLLM：
  App A ──► OpenAI SDK
  App B ──► DeepSeek HTTP
  App C ──► Ollama 本地
  → N 套协议、N 套 Key、N 套降级逻辑

有 LiteLLM：
  App A ──┐
  App B ──┼──► LiteLLM :4000 ──► 各上游模型
  App C ──┘
  → 一种 API、Key 集中、路由统一
```

**学习 LiteLLM 的第一目标**：理解「模型接入层」该长什么样，而不是死记某个厂商的 SDK。

---

## 二、架构与核心概念

### 2.1 整体架构

```text
┌──────────────┐     OpenAI Compatible      ┌──────────────┐
│ 客户端/业务   │ ─────────────────────────► │ LiteLLM :4000 │
└──────────────┘                             └──────┬───────┘
                                                    │
                    ┌───────────────────────────────┼───────────────────────────────┐
                    ▼                               ▼                               ▼
              ┌──────────┐                   ┌──────────┐                   ┌──────────┐
              │  OpenAI  │                   │ DeepSeek │                   │  Ollama  │
              └──────────┘                   └──────────┘                   └──────────┘
```

请求路径：

1. 客户端带 `model` 名称调用 `/v1/chat/completions`
2. LiteLLM 查 `model_list`，找到对应上游与参数
3. 转发上游，把响应转成 OpenAI 格式返回

### 2.2 三个必会配置概念

#### （1）`model_list` — 模型注册表

```yaml
model_list:
  - model_name: gpt-4o-mini          # 对外别名（客户端传的 model）
    litellm_params:
      model: openai/gpt-4o-mini      # LiteLLM 内部路由标识
      api_key: os.environ/OPENAI_API_KEY

  - model_name: deepseek-chat
    litellm_params:
      model: deepseek/deepseek-chat
      api_key: os.environ/DEEPSEEK_API_KEY

  - model_name: local-qwen
    litellm_params:
      model: ollama/qwen2.5
      api_base: http://localhost:11434
```

- **`model_name`**：调用方使用的名字
- **`litellm_params.model`**：实际上游（`openai/`、`deepseek/`、`ollama/` 等前缀）

#### （2）`general_settings.master_key` — 网关鉴权

```yaml
general_settings:
  master_key: sk-your-gateway-key
```

客户端请求 LiteLLM 时需带：

```http
Authorization: Bearer sk-your-gateway-key
```

**作用**：把「谁能调网关」和「网关调上游用的厂商 Key」分开，便于平台化。

#### （3）`litellm_settings` — 全局行为

```yaml
litellm_settings:
  drop_params: true    # 丢弃上游不支持的参数，减少报错
  set_verbose: false   # 日志详细程度
```

### 2.3 核心能力一览

| 能力 | 说明 | 学习优先级 |
|------|------|-----------|
| 统一 Chat API | `/v1/chat/completions` | ⭐⭐⭐ 必学 |
| 流式 Streaming | `stream: true` | ⭐⭐⭐ |
| Function Calling | tools / tool_calls | ⭐⭐ |
| Embeddings | `/v1/embeddings` | ⭐⭐ |
| Fallback / 路由 | 多模型降级 | ⭐⭐⭐ |
| Budget / Rate Limit | 预算与限流 | ⭐⭐ 生产必看 |
| Cache | 响应缓存 | ⭐⭐ |
| Admin UI | 可视化管理 | ⭐ 进阶 |

---

## 三、快速上手：第一次跑通 LiteLLM

### 3.1 安装与启动

**方式一：pip（本地学习最快）**

```bash
pip install "litellm[proxy]"
```

**方式二：Docker（接近生产）**

```bash
docker run -p 4000:4000 \
  -v $(pwd)/config.yaml:/app/config.yaml \
  ghcr.io/berriai/litellm:main-latest \
  --config /app/config.yaml --port 4000
```

创建 `config.yaml`：

```yaml
model_list:
  - model_name: gpt-4o-mini
    litellm_params:
      model: openai/gpt-4o-mini
      api_key: os.environ/OPENAI_API_KEY

general_settings:
  master_key: sk-demo-key
```

启动：

```bash
export OPENAI_API_KEY=sk-...
litellm --config config.yaml --port 4000
```

**Windows（本项目脚本）**：

```powershell
pip install "litellm[proxy]"
powershell -File deploy/scripts/start-litellm-local.ps1
```

### 3.2 健康检查

```bash
curl http://localhost:4000/health/liveliness
# 期望：200，如 "I'm alive!"
```

### 3.3 第一次 Chat 调用

```bash
curl http://localhost:4000/v1/chat/completions \
  -H "Authorization: Bearer sk-demo-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "用一句话介绍 LiteLLM"}],
    "max_tokens": 100
  }'
```

响应结构与 OpenAI 一致，重点看：

- `choices[0].message.content` — 模型回答
- `usage.prompt_tokens` / `completion_tokens` — 计量

**学到这一步**：你已经会用 LiteLLM 作为「统一出口」调任意已注册模型。

---

## 四、分层学习路径

### 4.1 入门：会配、会调、会排错

**目标**：独立搭一个 LiteLLM，接 1～2 个模型，curl 调通。

| 步骤 | 动作 |
|------|------|
| 1 | 写 `config.yaml`，配 `model_list` + `master_key` |
| 2 | 设置上游 Key 环境变量 |
| 3 | 启动 proxy，验 `/health/liveliness` |
| 4 | curl `/v1/chat/completions` |
| 5 | 看日志，区分「网关 401」vs「上游缺 Key」vs「模型不存在」 |

**常见错误对照**：

| 现象 | 原因 |
|------|------|
| `No api key passed in` | 调 LiteLLM 时未带 `Authorization: Bearer {master_key}` |
| `Missing credentials` / OpenAI 401 | 上游 `OPENAI_API_KEY` 未设置 |
| `model not found` | `model` 与 `model_name` 不一致 |

---

### 4.2 中级：多模型、降级、流式

#### 多模型切换

客户端只改 `model` 字段，网关按 `model_list` 路由，**无需改业务代码**。

#### Fallback（概念）

可在 LiteLLM 侧配置路由组、重试；也可在业务 / 控制面维护 `primary → fallback` 链，依次尝试。两种可并存：

- **网关层**：同一 `model_group` 内 failover
- **应用层**：显式 fallback 列表（如 ZestLLM Profile）

#### 流式调用

```bash
curl http://localhost:4000/v1/chat/completions \
  -H "Authorization: Bearer sk-demo-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "写一首短诗"}],
    "stream": true
  }'
```

返回 `data: {...}` SSE chunk，与 OpenAI 流式格式兼容。任何支持 OpenAI SDK 的客户端，把 `base_url` 指向 LiteLLM 即可。

#### 接本地 Ollama（零云 Key 学习）

```yaml
model_list:
  - model_name: local-chat
    litellm_params:
      model: ollama/llama3
      api_base: http://localhost:11434
```

适合本地学习 Function Calling、Prompt 工程，不产生 API 费用。

---

### 4.3 高级：生产化与扩展

| 主题 | 要点 |
|------|------|
| 密钥管理 | 上游 Key 用 `os.environ/XXX`，不进 Git |
| 限流 / Budget | `rpm`、`tpm`、团队预算（企业场景） |
| Redis 缓存 | `litellm_settings.cache` 降本增效 |
| 可观测 | 对接 Langfuse、Prometheus 等 |
| 稳定镜像 | 生产用 `-stable` tag（官方 12h 压测后再发布） |
| 高可用 | 多实例 + 负载均衡，网关无状态 |

**进阶心法**：LiteLLM 是 **Sidecar / 独立服务**，不要塞进业务 JVM；推理流量与控制逻辑分离，才易扩展。

---

## 五、LiteLLM 与同类工具对比（帮助选型）

| 维度 | LiteLLM | OneAPI | OpenRouter | 直连厂商 SDK |
|------|---------|--------|------------|-------------|
| 定位 | 自托管模型网关 | 自托管网关 | 云端聚合 SaaS | 应用内集成 |
| 协议 | OpenAI Compatible | OpenAI Compatible | OpenAI Compatible | 各厂商不同 |
| 自托管 | ✅ | ✅ | ❌ | N/A |
| 新模型跟进 | 快（社区活跃） | 较快 | 快（云端） | 跟 SDK 版本 |
| 许可证 | MIT | MIT | 商业 | — |
| 适合场景 | 平台 / 多服务 / 企业内网 | 类似 | 快速试用多模型 | 单应用 POC |

**学习 LiteLLM 的选型结论**：

- 要 **自托管、统一协议、长期维护多模型** → LiteLLM / OneAPI 一类
- 要 **快速试模型、不想运维** → OpenRouter
- 要 **一个 App 一个模型 POC** → 可先直连，规模化后再上网关

LiteLLM 的优势在于：**文档全、模型覆盖广、与 OpenAI 生态工具兼容好**，学习曲线平缓。

---

## 六、学 LiteLLM 要掌握的理论要点

### 6.1 为什么是 OpenAI Compatible？

OpenAI Chat Completions 已成为事实上的 **「HTTP + JSON 调用约定」**。LiteLLM 选这条路线，意味着：

- LangChain、OpenAI SDK、Cursor、各类 Agent 框架改 `base_url` 就能用
- 平台只需维护 **一套 HTTP 客户端**
- 换模型 = 换 `model` 字符串，不是换 SDK

### 6.2 两层 Key 模型（必理解）

```text
客户端 ──[master_key]──► LiteLLM ──[OPENAI_API_KEY 等]──► 上游
```

- **master_key**：谁可以调用你的网关
- **上游 Key**：网关代表你去调厂商

平台化项目几乎都会采用这种分层。

### 6.3 网关 vs 控制面

| | 模型网关（LiteLLM） | AI 控制面（如 ZestLLM） |
|---|---------------------|------------------------|
| 职责 | 连模型、路由、限流 | Prompt、发布、审计、Eval |
| 流量 | 重（token 流） | 轻（策略 JSON） |
| 配置 | model_list | Profile / 作业 code |

**两者互补，不是替代关系。** 学完 LiteLLM，再学控制面，架构会清晰很多。

---

## 七、学习检验清单

学完本文，你应能回答：

- [ ] LiteLLM Proxy 和 LiteLLM Python SDK 有什么区别？
- [ ] `model_name` 和 `litellm_params.model` 分别是什么？
- [ ] `master_key` 和 `OPENAI_API_KEY` 分别用在哪一段？
- [ ] 如何用 curl 调通 `/v1/chat/completions`？
- [ ] 网关 401 和上游缺 Key 如何区分？
- [ ] 为什么生产环境要把 LiteLLM 独立部署？
- [ ] 流式、Fallback 在网关层怎么理解？

---

## 八、附：ZestLLM 接入 LiteLLM 案例

> 以下不是本文主线，而是 **LiteLLM 在 Java AI 控制面中的落地示例**。  
> ZestLLM 是面向 Java 的 AI 作业调度与治理平台；LiteLLM 担任其 **默认模型网关**。

### 8.1 案例背景

ZestLLM 定位：**业务用 `@ZestLLM(code)` 声明 AI 作业，平台管 Prompt、模型策略与审计；推理走 LiteLLM，不经控制面转发 token。**

```text
业务 @ZestLLM
    │
    ├─ prepare ──► ZestLLM Admin :8088（Prompt、模型、traceId）
    │
    ├─ execute ──► LiteLLM :4000（实际推理）──► OpenAI / DeepSeek / …
    │
    └─ report  ──► ZestLLM Admin（token、cost、status）
```

### 8.2 LiteLLM 侧配置（项目内置）

路径：`deploy/litellm/config.yaml`

```yaml
model_list:
  - model_name: gpt-4o-mini
    litellm_params:
      model: openai/gpt-4o-mini
      api_key: os.environ/OPENAI_API_KEY

  - model_name: deepseek-chat
    litellm_params:
      model: deepseek/deepseek-chat
      api_key: os.environ/DEEPSEEK_API_KEY

general_settings:
  master_key: sk-zest-llm-demo

litellm_settings:
  drop_params: true
```

启动：

```powershell
# Windows 无 Docker
pip install "litellm[proxy]"
powershell -File deploy/scripts/start-litellm-local.ps1

# 有 Docker
cd deploy && docker compose up -d litellm
```

### 8.3 ZestLLM 侧配置

**Admin**（`zest-llm-admin/src/main/resources/application-local.yml`）：

```yaml
zest:
  llm:
    litellm:
      base-url: http://localhost:4000
      api-key: sk-zest-llm-demo   # 对应 LiteLLM master_key
```

**Agent Profile**（Admin UI 或 `deploy/examples/agent-profile-aichat.yaml`）：

```yaml
apiVersion: zestllm/v1
runtimeMode: agent
providerRef: litellm-default
model:
  primary: gpt-4o-mini
  fallback:
    - deepseek-chat
generation:
  maxTokens: 1024
  temperature: 0.7
  timeoutMs: 30000
outboundAuth:
  mode: API_KEY_REF
  secretRef: .env:LITELLM_API_KEY
providers:
  litellm-default:
    type: litellm
    baseUrl: http://localhost:4000   # 本地开发；Docker 内用 http://litellm:4000
    protocol: openai
```

**业务 Demo**（`zest-llm-demo/src/main/resources/application-local.yml`）：

```yaml
zest:
  llm:
    runtime-mode: agent
    control-plane-url: http://127.0.0.1:8088
    app-key: order-service
    auth-token: demo-token-123
    agent:
      enabled: true
      litellm-url: http://127.0.0.1:4000
      litellm-api-key: sk-zest-llm-demo
```

### 8.4 业务代码（与 LiteLLM 无关，体现网关价值）

```java
@ZestLLM(code = "aiChat", timeoutMs = 30000)
public AiChatResult aiChat(@AiInput("question") String question,
                           @AiOutput AiChatResult result) {
    // 模型、Prompt、Fallback 均在 Admin Profile 配置
    // execute 阶段由 Agent 直连 LiteLLM
    return result;
}
```

**要点**：业务不出现 `OpenAIClient`、`DeepSeekClient`；换模型只改 Admin Profile 和 LiteLLM `model_list`。

### 8.5 ZestLLM 如何调用 LiteLLM（SPI 封装）

```java
/**
 * 模型网关 SPI（默认 LiteLLM，可替换 OneAPI / Spring AI）。
 */
public interface ModelGatewayAdapter {
    String adapterId();           // 默认 "litellm"
    ChatResponse chat(ChatRequest request);
    HealthStatus health();        // GET /health/liveliness
}
```

实现类 `LiteLLMGatewayAdapter`（`zest-llm-infra` 模块）调用 `/v1/chat/completions`，并按 Profile 的 `primary → fallback` 依次尝试。

### 8.6 验证接入

```powershell
# 环境变量
$env:OPENAI_API_KEY = "sk-..."
$env:LITELLM_API_KEY = "sk-zest-llm-demo"

# 1. LiteLLM 健康
curl http://localhost:4000/health/liveliness

# 2. 启动 Admin（若未运行）
powershell -File deploy/scripts/start-local-full.ps1 -WithLiteLLM -SkipBuild

# 3. ZestLLM 调用
curl -H "Authorization: Bearer demo-token-123" `
     -H "Content-Type: application/json" `
     -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hello"}}' `
     http://localhost:8088/v1/llm/invoke

# 4. 一键修复 Provider 地址 + 测试（本地无 Docker）
powershell -File deploy/scripts/fix-local-litellm.ps1
```

成功标志：响应含 `traceId`、`output.answer`、`status: SUCCESS`。

### 8.7 案例小结

| 你在 LiteLLM 学到的 | 在 ZestLLM 中的体现 |
|--------------------|---------------------|
| `model_list` | Profile `model.primary` / `fallback` |
| `master_key` | `LITELLM_API_KEY` / `litellm.api-key` |
| OpenAI Compatible API | `LiteLLMGatewayAdapter` 统一 HTTP 调用 |
| 独立 Sidecar 部署 | prepare/report 走 Admin，execute 直连 LiteLLM |
| 多模型路由 | 网关 + Profile 双层 fallback |

**这一案例说明**：学完 LiteLLM 后，在 Java 平台型项目里，通常把它放在 **execute 层**；ZestLLM 是其中一种标准接法，不是 LiteLLM 的唯一用法。

---

## 九、总结

| 层次 | 你要带走什么 |
|------|-------------|
| 认知 | LiteLLM 是模型网关，不是模型本身 |
| 配置 | `model_list`、`master_key`、环境变量 |
| 调用 | OpenAI Compatible HTTP，curl / SDK 改 base_url 即可 |
| 进阶 | 流式、Fallback、缓存、限流、独立部署 |
| 实践 | ZestLLM 案例：控制面 + LiteLLM 网关的分工 |

**学习路径建议**：先用 pip + curl 把 LiteLLM 跑通 → 接第二个模型 + Ollama → 理解两层 Key → 再读 ZestLLM 案例看平台化落地。

---

## 参考

- LiteLLM 官方文档：<https://docs.litellm.ai>
- LiteLLM GitHub：<https://github.com/BerriAI/litellm>
- 本项目配置：`deploy/litellm/config.yaml`
- 本项目验收：`deploy/scripts/full-acceptance.ps1`

---

> **版权声明**：LiteLLM 采用 MIT 许可证；ZestLLM 案例基于 Apache-2.0 开源实践。转载请注明出处。  
> **标签**：`LiteLLM` `模型网关` `大模型接入` `OpenAI Compatible` `LLM Proxy` `ZestLLM` `Java AI`
