package cn.zest.www.zestllm.spi.adminsso;

/**
 * Admin SSO 提供方 SPI — 支持 ZestSSO、通用 OIDC 等可插拔实现。
 */
public interface AdminSsoProvider {

    /** 提供方标识，如 zest-sso、oidc、none */
    String providerId();

    AdminSsoPublicConfig buildPublicConfig(AdminSsoProviderConfig config);

    AdminSsoAuthorizeInfo buildAuthorizeUrl(AdminSsoProviderConfig config);

    /** 单点登出 URL；未启用时由门面返回 null */
    String buildLogoutUrl(AdminSsoProviderConfig config);

    AdminSsoLoginResult handleCallback(AdminSsoCallbackInput input, AdminSsoProviderConfig config);

    /**
     * 可选：直接交换 id_token（部分 IdP 前端直传 id_token 时使用）。
     * 默认不支持，由具体 OIDC 提供方覆盖。
     */
    default AdminSsoLoginResult exchangeIdToken(String idToken, AdminSsoProviderConfig config) {
        throw new UnsupportedOperationException("exchangeIdToken not supported for provider " + providerId());
    }
}
