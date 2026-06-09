# zest-llm-admin — AI 辅助开发 MCP 接入

> 平台 JAR 一次安装：`powershell -File <zestflow>/scripts/dev/install-mcp.ps1`  
> 完整对照表见 zestflow 仓库 [docs/AI_IDE_SETUP.md](https://gitee.com/zestcc/zestflow/blob/master/docs/AI_IDE_SETUP.md)

| 项 | 值 |
|----|-----|
| appCode | **zest-llm-admin** |
| Executor | **http://127.0.0.1:20550** |
| 工程路径 | `D:/WORK/Project/zestLLM` |

## 启动前

1. JDK **17+**（MCP Server）；JDK **8+** 仅 init-dev
2. 启动本业务应用，确认 Netty 端口与 **http://127.0.0.1:20550** 一致
3. 验证：`curl http://127.0.0.1:20550/health` 或 MCP Tool `list_components`

---

## Cursor

- 配置：`.cursor/mcp.json`（已生成）
- 打开 **本工程根目录** → 设置 → MCP → 刷新
- Agent 规则：`.cursor/rules/zestflow-architecture.md`

## VS Code Copilot / Cline

- 配置：`.vscode/mcp.json`（已生成）
- Cline 扩展同样读取 `.vscode/mcp.json`
- Copilot 指令：`.github/copilot-instructions.md`

## Claude Desktop

1. 复制 `.zestflow/mcp/claude-desktop.config.json.example` 中 `mcpServers.zestflow` 块
2. 粘贴到用户配置：
   - Windows：`%APPDATA%\Claude\claude_desktop_config.json`
   - macOS：`~/Library/Application Support/Claude/claude_desktop_config.json`
3. 完全退出并重启 Claude Desktop

## Claude Code（CLI）

- 配置：项目根 **`.mcp.json`**（已生成，可提交 Git）
- 在本工程目录执行 `claude`，首次会提示批准 project-scoped MCP
- 若未加载：`claude mcp reset-project-choices` 后重启会话
- 协作规范：`CLAUDE.md`

## Windsurf（Cascade）

Windsurf **仅支持全局** MCP，无项目级配置：

1. 打开 `%USERPROFILE%\.codeium\windsurf\mcp_config.json`（或 Cascade → MCP → Configure）
2. 将 `.zestflow/mcp/windsurf.mcp_config.json.example` 中 `mcpServers` **合并**进去
3. 保存后点 Cascade MCP 面板的 **Refresh**

## 可选：Executor 鉴权

若配置了 `zestflow.executor.access-token`，在 MCP `args` 末尾追加：

```json
"--executor-token", "YOUR_TOKEN"
```

## 推荐工作流

```text
search_patterns → plan_chain → scaffold_component → validate_chain
  → gen_playground_scene → record_learning_event
```

写源码由 IDE Apply；MCP 不写盘。发布链在 Admin 人工操作。
