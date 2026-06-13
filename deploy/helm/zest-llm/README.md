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

### 生产示例 values

复制并按环境改写（**勿提交真实密钥**）：

```bash
helm upgrade --install zest-llm ./deploy/helm/zest-llm \
  -f deploy/helm/zest-llm/values-production.example.yaml \
  --set mysql.existingSecret=zest-llm-mysql
```

`values-production.example.yaml` 涵盖：Ingress + TLS、外部 MySQL/Valkey Secret、SSO 配置占位、Admin 资源 limits、HPA。

## 主要 Values

| Key | 说明 | 默认 |
|-----|------|------|
| `mysql.existingSecret` | 使用外部 Secret 存 DB 密码 | `""` |
| `litellm.enabled` | 部署 LiteLLM Deployment | `false` |
| `demo.enabled` | 部署 Demo 应用 | `true` |
| `ingress.enabled` | 暴露 Admin（及 Demo `/demo`） | `false` |
| `autoscaling.enabled` | Admin HPA | `false` |
| `probes.admin.*` | Admin 存活/就绪探针路径 | `/swagger-ui.html` |
| `values-production.example.yaml` | 生产参考（SSO/Secret/Ingress/limits） | 见文件 |

## 压测

```bash
# production 门禁 P95≤500ms
TIER=production ADMIN_URL=http://localhost:8088 REQUESTS=200 CONCURRENCY=30 \
  bash deploy/scripts/loadtest-cp-prepare.sh

# local 调试 P95≤800ms
TIER=local P95_MAX_MS=800 bash deploy/scripts/loadtest-cp-prepare.sh
```

## Large Tier（Compose 对照）

Helm 当前为 Admin/Demo/LiteLLM 最小集；Dify/RAGFlow/Kafka 见 Docker Large Tier：

```bash
bash deploy/scripts/validate-large-tier-compose.sh   # 干跑校验
bash deploy/scripts/zest-stack-up.sh large         # 全栈启动
```

## Kafka Report（可选）

```bash
cd deploy
docker compose -f docker-compose.yml -f docker-compose.kafka.yml up -d
```

Admin 将使用 `docker,kafka` profile，`report-channel: kafka`。
