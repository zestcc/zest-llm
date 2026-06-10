# 场景模板示例（与 Admin classpath scenario-templates 同步）

| 目录 | 场景 | 推荐 Tier |
|------|------|-----------|
| `chat/` | 智能对话 | small |
| `report/` | 报表解读 | medium |
| `ops/` | 运维诊断 | large |

## 使用方式

1. Admin → **场景模板** → 选择模板 → 应用
2. 或 API：`POST /api/admin/scenario-templates/apply`

```json
{
  "templateId": "chat-basic",
  "appKey": "order-service",
  "taskCode": "aiChat",
  "publish": false
}
```

3. 业务侧 `@ZestLLM(code = "aiChat")` 即可调度

详见 [docs/Zest-Stack完整实现方案.md](../../docs/Zest-Stack完整实现方案.md)
