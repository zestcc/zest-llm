# MCP 接入示例

ZestLLM 通过 Profile `tools[].type=mcp` 声明 MCP 工具，由 `HttpMcpToolAdapter` 调用 JSON-RPC。

## 本地 Mock（无需自建 Server）

Docker 栈内置 `mcp-mock`（WireMock），Profile 示例见 `deploy/examples/agent-profile-aichat.yaml`。

```yaml
tools:
  - type: mcp
    name: search
    serverRef: internal-docs
    config:
      toolName: search
```

Admin 中注册 MCP Server：`POST /api/admin/mcp-servers`（或在智能体配置页维护）。

## 生产接入步骤

1. 部署 MCP Server（只读工具：查报表、查日志）
2. Admin 注册 `serverRef` → baseUrl
3. Profile 声明 tools + `toolCallMode: loop`
4. 推荐使用 **large** 栈：`zest-stack-up.ps1 -Tier large`

## 参考

- `.zestflow/mcp/README.md` — IDE MCP 规范
- `deploy/mcp-mock/` — WireMock mappings
- 场景模板 `ops-monitor` — 含 MCP + Dify + RAGFlow extensions
