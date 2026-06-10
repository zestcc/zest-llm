# Spring AI 与 ZestLLM

## 分工

| | Spring AI | ZestLLM |
|---|-----------|---------|
| 层级 | 应用内 SDK | 应用外控制面 |
| 模型调用 | `ChatClient` / `ChatModel` | LiteLLM Gateway |
| 工具 | `@Tool` / Function Calling | MCP + Profile tools |
| 配置 | 代码 / application.yml | Admin Profile SSOT |
| 治理 | 需自建 | Eval / Probe / FinOps 内置 |

## 推荐共存方式

1. 业务主路径：`@ZestLLM(code)` → Control Plane prepare/invoke/report  
2. 应用内补充：Spring AI 用于非治理型实验代码  
3. 不在 ZestLLM 内重复实现 Spring AI 全部抽象；可选未来 `ModelGatewayAdapter=spring-ai`

## 一句话

Spring AI 是 **Driver**；ZestLLM 是 **Scheduler + Governance**。
