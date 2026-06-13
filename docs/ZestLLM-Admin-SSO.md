# ZestLLM Admin SSO 集成指南

> 可插拔 Admin 单点登录（BFF + PKCE），对齐 ZestFlow 市场级 SSO SPI 模式。

---

## 1. 架构概览

```
LoginView → /api/admin/auth/sso/* (或 legacy /oidc/*)
         → AdminSsoAuthService（门面）
         → AdminSsoProviderRegistry（按配置选提供方）
         → ZestSsoAdminProvider | GenericOidcAdminProvider | DisabledAdminSsoProvider
         → OidcEndpointResolver + OidcTokenClient + AdminSsoPkceStore
         → OidcJwtValidator + AdminSsoUserProvisioner → JWT
```

- **SPI**：`zest-llm-spi` 中 `AdminSsoProvider` 接口
- **PKCE 存储**：无 Redis 时默认 `InMemoryAdminSsoPkceStore`；配置 `spring.data.redis.host` 时用 `RedisAdminSsoPkceStore`
- **向后兼容**：`zest-llm.admin.oidc.*` 在 `sso.client-id` 为空时自动合并到 `sso.*`；`/api/admin/auth/oidc/*` 仍可用

---

## 2. 可插拔提供方

| provider 值 | 实现类 | 说明 |
|-------------|--------|------|
| `zest-sso` | `ZestSsoAdminProvider` | ZestSSO Discovery + logout-url API（默认） |
| `oidc` | `GenericOidcAdminProvider` | 通用 OIDC（Keycloak、Authing 等） |
| `none` / `enabled=false` | `DisabledAdminSsoProvider` | 关闭 SSO，仅本地账号登录 |

配置前缀：`zest-llm.admin.sso`

---

## 3. 本地启用 ZestSSO

### 3.1 前置

1. 启动 MySQL + ZestLLM Admin（`deploy/scripts/start-local-full.ps1`）
2. 启动 **ZestSSO**（`:9000`）— 见 `D:\project\zest\zest-sso` 项目 README
3. 在 ZestSSO 注册 OAuth Client：`zest-llm-admin`，redirect_uri 与下方一致

### 3.2 配置（application-local.yml）

复制 `application-local.example.yml` 中 `sso` 块，将 `enabled` 改为 `true`：

```yaml
zest-llm:
  admin:
    sso:
      enabled: true
      provider: zest-sso
      client-id: zest-llm-admin
      client-secret: <ZestSSO 中该 client 的 secret>
      redirect-uri: http://localhost:5174/login/callback   # Vite dev
      # 内嵌 UI: http://127.0.0.1:8088/login/callback
      discovery-uri: http://localhost:9000/api/public/.well-known/openid-configuration
```

### 3.3 验证

```powershell
powershell -File deploy/scripts/sso-smoke.ps1 -AdminUrl http://localhost:8088 -SsoBase http://localhost:9000
```

浏览器打开 `http://localhost:5174/login`（或 `:8088`），点击 **ZestSSO 登录**，完成回调后检查 `llm_admin_user` 表 `sso_provider` / `sso_subject`。

### 3.4 本地账号不受影响

`enabled: false`（默认）时，仍可使用 `admin` / `admin123` 本地登录。

### 3.5 浏览器联调：验证 `sso_subject` 已写入

完成 SSO 登录回调后，在 MySQL 中确认用户映射（**浏览器步骤不可被 `sso-smoke.sh` 替代**）：

1. 确认 Admin 已启用 SSO（`zest-llm.admin.sso.enabled=true`）且 ZestSSO 可达。
2. 浏览器打开登录页（Vite `:5174/login` 或内嵌 UI `:8088/login`），点击 **ZestSSO 登录**。
3. 在 IdP 完成认证，确认跳回 `/login/callback` 且无报错。
4. 登录后访问需鉴权页面（如「用户管理」），确认已拿到 Admin JWT。
5. 查询数据库（替换为实际 `sub` / 邮箱）：

```sql
SELECT id, username, sso_provider, sso_subject, email, updated_at
FROM llm_admin_user
WHERE sso_provider IS NOT NULL
ORDER BY updated_at DESC
LIMIT 10;
```

6. **通过标准**：最新登录用户行中 `sso_provider` 为 `zest-sso`（或配置的 provider），`sso_subject` 非空且与 IdP `sub` claim 一致。
7. 登出后重登：同一 IdP 用户应更新 `updated_at`，`sso_subject` 保持不变（upsert 幂等）。

---

## 4. API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/auth/sso/config` | 公开配置（enabled、provider、displayName） |
| GET | `/api/admin/auth/sso/authorize` | 生成 PKCE 授权 URL |
| POST | `/api/admin/auth/sso/callback` | 授权码换 JWT |
| POST | `/api/admin/auth/sso/exchange` | 直接交换 id_token（可选） |
| GET | `/api/admin/auth/sso/logout-url` | 单点登出 URL |

Legacy 路径 `/api/admin/auth/oidc/*` 行为相同。

---

## 5. ZestFlow 参考实现的已知不足

移植自 ZestFlow `com.zestflow.admin.service.sso.*` 时需注意：

| 项 | 说明 |
|----|------|
| redirect-uri 文档不一致 | ZestFlow 文档与示例 yml 中 dev redirect 端口不统一（5173 vs 8080） |
| 无 exchange 端点 | ZestFlow `SsoAuthService` 未暴露 `/exchange`；ZestLLM 保留以兼容旧集成 |
| PKCE 仅 cluster Redis | 旧版 ZestLLM 仅 `RedisAdminSsoPkceStore`，本地无 Redis 无法 SSO；现已默认 InMemory |
| OIDC 代码与 SDK 重复 | ZestFlow Admin 内嵌 OIDC 客户端，未复用独立 SDK 模块 |
| 租户映射简化 | claims 中 `tenant_id` 未映射到 ZestLLM 多租户模型，仅作 JWT 展示 |

---

## 6. 生产上线检查清单

- [ ] `spring.profiles.active` 含 `prod`，`AdminProductionGuard` 通过
- [ ] `zest-llm.admin.sso.enabled=true`
- [ ] `client-secret` 已更换（非 `change-me*` 占位符）
- [ ] `client-id`、`redirect-uri`、`post-logout-redirect-uri` 与 IdP 注册一致
- [ ] 内嵌 UI 生产 redirect：`http(s)://<admin-host>/login/callback`
- [ ] 多实例部署时配置 `spring.data.redis.host`（PKCE state 共享）
- [ ] ZestSSO client `zest-llm-admin` 已在 IdP 侧创建
- [ ] 运行 `deploy/scripts/sso-smoke.ps1` 与 `production-acceptance.ps1`（含 SSO 段）
- [ ] 浏览器联调后执行 [sso-browser-checklist.md](../deploy/scripts/sso-browser-checklist.md) 中的 SQL / `sso-db-verify.sh`

---

## 7. 相关文件

| 路径 | 说明 |
|------|------|
| `zest-llm-spi/.../adminsso/AdminSsoProvider.java` | SPI 接口 |
| `zest-llm-admin/.../config/AdminSsoProperties.java` | 配置绑定 |
| `zest-llm-admin/.../service/sso/` | 提供方、Registry、AuthService、**Back-Channel Logout 吊销** |
| `zest-llm-admin-ui/src/views/LoginView.vue` | SSO 登录按钮 |
| `application-local.example.yml` | 本地配置示例 |
| `application-production.example.yml` | 生产 SSO 块 |
| `deploy/scripts/sso-browser-checklist.md` | 浏览器联调 + SQL/curl 复制粘贴 |
| `deploy/scripts/sso-db-verify.sh` | 只读验证 `sso_subject`（需 `MYSQL_*`） |
