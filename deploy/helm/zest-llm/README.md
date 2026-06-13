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
| `admin.sso.enabled` | 启用 Admin SSO（OIDC/ZestSSO） | `false` |
| `admin.sso.existingSecret` | SSO client-secret 的 K8s Secret | `""` |
| `admin.jwt.existingSecret` | JWT 签名密钥 Secret | `""` |
| `values-production.example.yaml` | 生产参考（SSO/Secret/Ingress/limits） | 见文件 |

## Admin SSO 与多副本

启用 `admin.sso.enabled=true` 时：

1. **必须配置 Redis（Valkey）**：PKCE state 与 Back-Channel Logout 会话吊销依赖 `SPRING_DATA_REDIS_*`（Chart 已注入 `redis.host` / `redis.port`）。
2. **多副本（`replicaCount` > 1 或 HPA）**：所有 Admin Pod 须指向**同一** Redis 实例，否则 SSO 回调与登出吊销会不一致。
3. **密钥**：`client-secret` 与 JWT 通过 `admin.sso.existingSecret`、`admin.jwt.existingSecret` 注入，勿写入 values Git。

示例（生产 values 片段）：

```yaml
replicaCount: 2
redis:
  host: valkey.prod.svc.cluster.local
  port: 6379
admin:
  sso:
    enabled: true
    provider: zest-sso
    issuer: https://sso.example.com
    discoveryUri: https://sso.example.com/api/public/.well-known/openid-configuration
    clientId: zest-llm-admin
    existingSecret: zest-llm-sso
    secretClientSecretKey: client-secret
    redirectUri: https://zest-llm.example.com/login/callback
    postLogoutRedirectUri: https://zest-llm.example.com/login
  jwt:
    existingSecret: zest-llm-jwt
    secretKey: secret
```

对应 Deployment 环境变量：`ZEST_LLM_ADMIN_SSO_*`、`ZEST_LLM_ADMIN_JWT_SECRET`（Spring Boot relaxed binding → `zest-llm.admin.sso.*`）。

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
