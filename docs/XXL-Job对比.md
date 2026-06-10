# ZestLLM 与 XXL-Job 对照

| 维度 | XXL-Job | ZestLLM |
|------|---------|---------|
| 定位 | 分布式任务调度 | AI 作业调度与治理 |
| 作业声明 | `@XxlJob("handler")` | `@ZestLLM("code")` |
| 配置中心 | Admin 任务 / Cron | Profile + Prompt + Route |
| 执行器 | 业务 JVM 注册 | Starter 方法注册 + Agent |
| 后端引擎 | 无（业务自己跑） | LiteLLM / Dify / RAGFlow / MCP |
| 发布门禁 | 无 | Eval + Probe |
| 可观测 | 调度日志 | Execution + Langfuse |
| 适用 | 定时/异步任务 | LLM 调用治理 |

**结论**：XXL-Job 调度「何时跑」；ZestLLM 调度「AI 怎么跑、跑什么模型、能否发布」。
