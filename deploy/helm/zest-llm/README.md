# ZestLLM Helm Chart

方案 A Phase2 最小 Helm 部署（Admin CP）。

## 安装

```bash
helm upgrade --install zest-llm ./deploy/helm/zest-llm \
  --set mysql.host=your-mysql \
  --set redis.host=your-valkey
```

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
