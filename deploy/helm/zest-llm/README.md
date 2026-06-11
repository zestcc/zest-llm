# ZestLLM Helm Chart

方案 A Phase2 Helm 部署（Admin CP + 可选 Demo / LiteLLM）。

## 安装

```bash
helm upgrade --install zest-llm ./deploy/helm/zest-llm \
  --set mysql.host=your-mysql \
  --set redis.host=your-valkey \
  --set demo.enabled=true \
  --set litellm.enabled=true \
  --set ingress.enabled=true \
  --set ingress.host=zest-llm.example.com
```

## 主要 Values

| Key | 说明 | 默认 |
|-----|------|------|
| `mysql.existingSecret` | 使用外部 Secret 存 DB 密码 | `""` |
| `litellm.enabled` | 部署 LiteLLM Deployment | `false` |
| `demo.enabled` | 部署 Demo 应用 | `true` |
| `ingress.enabled` | 暴露 Admin（及 Demo `/demo`） | `false` |
| `autoscaling.enabled` | Admin HPA | `false` |
| `probes.admin.*` | Admin 存活/就绪探针路径 | `/swagger-ui.html` |

## 压测

```bash
ADMIN_URL=http://localhost:8088 REQUESTS=200 CONCURRENCY=20 P95_MAX_MS=500 ./deploy/scripts/loadtest-cp-prepare.sh
```

## Kafka Report（可选）

```bash
cd deploy
docker compose -f docker-compose.yml -f docker-compose.kafka.yml up -d
```

Admin 将使用 `docker,kafka` profile，`report-channel: kafka`。
