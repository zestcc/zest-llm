# zest-llm-admin MCP 项目规则（L2）

> **架构与元件化**见 `.zestflow/rules/architecture.md`（IDE 通用基线）。本文件仅补充 MCP / 学习沉淀约定。

## MCP 连接

- 初始化 CLI（Java 8+）：`~/.zestflow/tools/zestflow-dev-init.jar`
- MCP Server（Java 17+）：`~/.zestflow/tools/zestflow-mcp.jar`
- **按 IDE 选配置**（详见 `.zestflow/mcp/README.md`）：
  - Cursor → `.cursor/mcp.json`
  - VS Code / Cline → `.vscode/mcp.json`
  - Claude Code → `.mcp.json`（项目根）
  - Claude Desktop → `.zestflow/mcp/claude-desktop.config.json.example` → 用户目录
  - Windsurf → `.zestflow/mcp/windsurf.mcp_config.json.example` → 全局 `mcp_config.json`
- appCode：**zest-llm-admin**，Executor：**http://127.0.0.1:20550**
- 元件化：**full**，HTTP Mode：**3**

## AI 生成唯一规则（`ai-generation-acceptance`）

- **链条知识库在应用端 Executor**：`{dataDir}/ai/patterns/` + `learning/events.jsonl`；Admin/MCP 通过 HTTP 代理，不存业务链蒸馏。
- **验收标准生成**：检索应用端 RAG → 对标业界 → validate → `record_learning_event` → **自动蒸馏**。
- MCP 须配置 `--executor-url`；Admin 设计器 Copilot 自动拉取应用端 RAG。

## 新功能标准工作流（完整交付）

```text
plan_chain
  → scaffold_component（元件 + Repo + DTO/VO + Mode3 Controller 若需）
  → compose_chain + validate_chain
  → 设计图 graph_data 与链同步（设计器节点与元件 id 一致）
  → bind_http / @ZestChain
  → gen_playground_scene → record_learning_event
```

- **禁止**只提交单个 Handler 而无链、无设计图、无 HTTP 绑定（除非用户明确仅改元件）。
- 链与设计图草稿由 MCP 生成；Admin 发布需人工确认。

## Chain-first 学习

- 原始事件：`.zestflow/learning/events.jsonl`
- 蒸馏 Pattern：`.zestflow/patterns/`（建议提交 Git）

## 推荐链式调用

```text
plan_chain → scaffold_component → validate_chain → gen_playground_scene
  → record_learning_event → distill_patterns
```
