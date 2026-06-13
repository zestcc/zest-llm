# 15 分钟 Demo 路径

## 前置（Windows 无 Docker）

需本机 **MySQL 8**（`:3306`）已启动；首次请复制 `application-local.example.yml` → `application-local.yml` 并改数据库密码。

```powershell
pip install "litellm[proxy]"
Copy-Item zest-llm-admin/src/main/resources/application-local.example.yml `
          zest-llm-admin/src/main/resources/application-local.yml

# 推荐：Admin + mock LiteLLM + 业务 Demo（一条命令）
powershell -File deploy/scripts/start-local-full.ps1 -EmbedUi -WithLiteLLM -WithDemo

# 可选 MCP Tool Loop 冒烟
# powershell -File deploy/scripts/start-local-full.ps1 -EmbedUi -WithLiteLLM -WithDemo -WithMcpMock
```

| 入口 | 地址 |
|------|------|
| Admin | http://127.0.0.1:8088 （admin / admin123） |
| Demo methodA | http://127.0.0.1:8081/demo/order/methodA?orderId=1&question=hi |
| LiteLLM | http://127.0.0.1:4000 |

停止：`powershell -File deploy/scripts/start-local-full.ps1 -StopOnly`

> 若团队有 Linux 测试机，全栈 AC1–38 可在该环境用 compose 跑 `e2e-acceptance.sh`；**不要求 Windows 本机安装 Docker**。

## 步骤（约 15 分钟）

| 分钟 | 动作 | 验证 |
|------|------|------|
| 0–2 | 登录 → **平台能力 → 能力栈** | 看到 small Tier + SPI 健康 |
| 2–5 | **场景模板** 或 **AI 作业 → 从模板创建** → 应用 `chat-basic` | 生成 Profile 草稿 |
| 5–8 | **AI 作业 → 智能体配置** → 发布预览 → 发布 | Eval/Probe 门禁提示 |
| 8–11 | 浏览器或 curl 调 Demo `methodA` | 返回 `answer` + `traceId` |
| 11–13 | **执行记录** + **自我改进** | suggest-cases / Execution 可查 |
| 13–15 | `powershell -File deploy/scripts/full-acceptance.ps1` | 0 FAIL（含 DEMO-01） |

## 一键脚本

```powershell
powershell -File deploy/scripts/demo-walkthrough.ps1
powershell -File deploy/scripts/verify-local.ps1
```

## 异步长任务

业务拿到 `traceId` 后轮询：

```powershell
curl -H "Authorization: Bearer demo-token-123" `
  http://127.0.0.1:8088/v1/executions/{traceId}
```
