# Admin SSO 浏览器联调清单

> 自动化 `sso-smoke.sh` / `sso-smoke.ps1` **不能**替代浏览器回调与 `sso_subject` 落库验证。  
> 详细配置见 [ZestLLM-Admin-SSO.md](../../docs/ZestLLM-Admin-SSO.md)。

---

## 前置

1. Admin 已启动（`start-local-full.ps1` 或 Docker 栈）。
2. `zest-llm.admin.sso.enabled=true`，`client-secret` 非占位符。
3. ZestSSO（`:9000`）可达，OAuth Client `zest-llm-admin` 已注册。

---

## 浏览器步骤

| # | 操作 | 通过标准 |
|---|------|----------|
| 1 | 打开 `http://localhost:5174/login`（Vite）或 `http://127.0.0.1:8088/login`（内嵌 UI） | 页面显示 **ZestSSO 登录** |
| 2 | 点击 SSO 登录，在 IdP 完成认证 | 跳回 `/login/callback`，无报错 |
| 3 | 进入「用户管理」等需鉴权页面 | 已持有 Admin JWT，API 200 |
| 4 | 登出后重登同一 IdP 用户 | `updated_at` 更新，`sso_subject` 不变 |

---

## 复制粘贴：SQL 验证 `sso_subject`

```sql
-- 替换库名；最近 SSO 登录用户
SELECT id, username, sso_provider, sso_subject, email, updated_at
FROM llm_admin_user
WHERE sso_provider IS NOT NULL
ORDER BY updated_at DESC
LIMIT 10;
```

**通过**：`sso_provider` = `zest-sso`（或配置的 provider），`sso_subject` 非空且与 IdP `sub` claim 一致。

按 subject 精确查：

```sql
SELECT id, username, sso_provider, sso_subject, email, updated_at
FROM llm_admin_user
WHERE sso_subject = '<IdP-sub-claim>';
```

---

## 复制粘贴：curl 冒烟（无浏览器）

```bash
# Discovery（ZestSSO 未启时 WARN，不阻断签字）
curl -sf http://localhost:9000/api/public/.well-known/openid-configuration | head -c 200

# Admin SSO 配置
curl -sf http://localhost:8088/api/admin/auth/sso/config

# Authorize URL（enabled=true 时应有 authorizationUrl + state）
curl -sf http://localhost:8088/api/admin/auth/sso/authorize
```

Windows：

```powershell
powershell -File deploy/scripts/sso-smoke.ps1 -AdminUrl http://localhost:8088 -SsoBase http://localhost:9000
```

---

## 只读 DB 脚本（可选）

```bash
# 需 mysql 客户端 + MYSQL_* 环境变量
bash deploy/scripts/sso-db-verify.sh
```

环境变量：`MYSQL_HOST`（默认 127.0.0.1）、`MYSQL_PORT`（3306）、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE`（默认 `zest_llm`）。
