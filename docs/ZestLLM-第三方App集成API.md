# ZestLLM 第三方 App 集成 API

面向 ZestStory 等外部消费者的 **Runtime** 查询接口（Bearer token 鉴权，与 `/v1/llm/invoke` 一致）。

## 端点

```
GET /v1/apps/{appKey}/integration-status
GET /v1/apps/{appKey}/tasks
GET /v1/apps/{appKey}/tasks/{code}/availability?smokeTest=false
```

## 示例

```bash
TOKEN=zeststory-runtime-dev-token
APP=zeststory

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8088/v1/apps/$APP/integration-status" | jq .

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8088/v1/apps/$APP/tasks/zestStoryInvoke/availability?smokeTest=true" | jq .
```

## DTO

`zest-llm-common` → `cn.zest.www.zestllm.common.api.integration`

- `AppIntegrationStatusResponse`
- `AppTaskSummary`
- `AppTaskAvailabilityResponse`
- `AppIntegrationCheck`

## 实现

- Controller: `RuntimeAppIntegrationController`
- Service: `AppIntegrationService`（聚合任务、已发布 Profile、最近 Probe 记录；availability 委托 `AgentProfileProbeService`）
