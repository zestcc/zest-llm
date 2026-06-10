# 15 分钟 Demo 路径

## 前置

```powershell
powershell -File deploy/scripts/zest-stack-up.ps1 -Tier small
# 或本地：mvn -pl zest-llm-admin spring-boot:run -Dspring-boot.run.profiles=local
```

Admin: http://localhost:8088 （admin / admin123）

## 步骤（约 15 分钟）

| 分钟 | 动作 | 验证 |
|------|------|------|
| 0–2 | 登录 → **平台能力 → 能力栈** | 看到 small Tier + SPI 健康 |
| 2–5 | **场景模板** → 应用 `chat-basic` → 勾选 Probe | 生成 Profile 草稿 |
| 5–8 | **AI 作业 → 智能体配置** → 发布预览 → 发布 | Eval/Probe 门禁提示 |
| 8–11 | Demo 调用 `GET /demo/order/methodA` 或 prepare | traceId 返回 |
| 11–13 | **执行记录** + **自我改进**（Execution/Langfuse 样本） | suggest-cases 有数据 |
| 13–15 | `powershell -File deploy/scripts/full-acceptance.ps1` | 0 FAIL |

## 一键脚本

```powershell
powershell -File deploy/scripts/demo-walkthrough.ps1
```

## 异步长任务

业务拿到 `traceId` 后轮询：

```bash
curl -H "Authorization: Bearer demo-token-123" \
  http://localhost:8088/v1/executions/{traceId}
```
