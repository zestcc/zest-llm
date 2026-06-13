package cn.zest.www.zestllm.admin.config;

import cn.zest.www.zestllm.spi.adminsso.AdminSsoProviderConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * ZestLLM Admin SSO 集成配置。
 * <p>
 * provider 可选：zest-sso（ZestSSO）、oidc（通用 OIDC）、none（关闭）。
 */
@Data
@ConfigurationProperties(prefix = "zest-llm.admin.sso")
public class AdminSsoProperties implements AdminSsoProviderConfig {

    private boolean enabled = false;

    /** 提供方标识：zest-sso | oidc | none */
    private String provider = "zest-sso";

    /** 前端按钮展示名（oidc 提供方可覆盖） */
    private String displayName;

    private String issuer = "http://localhost:9000";

    /** OIDC Discovery 地址；ZestSSO 默认 /api/public/.well-known/openid-configuration */
    private String discoveryUri = "http://localhost:9000/api/public/.well-known/openid-configuration";

    private String clientId;
    private String clientSecret = "change-me-in-production";
    private String redirectUri = "http://localhost:5174/login/callback";
    private String postLogoutRedirectUri = "http://localhost:5174/login";
    private List<String> scopes = List.of("openid", "profile", "email", "roles", "tenant");
    private String audience = "zest-llm-admin";
    private String jwksUri = "http://localhost:9000/oauth2/jwks";

    private SsoClaimsProperties claims = new SsoClaimsProperties();
    private ZestSsoProperties zestSso = new ZestSsoProperties();

    @Override
    public String getUsernameClaim() {
        return claims.getUsernameClaim();
    }

    @Override
    public String getRolesClaim() {
        return claims.getRolesClaim();
    }

    @Override
    public String getAdminRole() {
        return claims.getAdminRole();
    }

    @Override
    public String getOperatorRole() {
        return claims.getOperatorRole();
    }

    @Override
    public String getDefaultRole() {
        return claims.getDefaultRole();
    }

    @Override
    public boolean isZestSsoUseLogoutUrlApi() {
        return zestSso.isUseLogoutUrlApi();
    }

    @Override
    public String getZestSsoLogoutUrlApiPath() {
        return zestSso.getLogoutUrlApiPath();
    }

    @Data
    public static class SsoClaimsProperties {
        private String usernameClaim = "preferred_username";
        private String rolesClaim = "roles";
        private String adminRole = "SSO_ADMIN";
        private String operatorRole = "SSO_OPERATOR";
        private String defaultRole = "ADMIN";
    }

    @Data
    public static class ZestSsoProperties {
        /** 是否调用 ZestSSO /api/public/logout-url 获取登出地址 */
        private boolean useLogoutUrlApi = true;
        private String logoutUrlApiPath = "/api/public/logout-url";
    }
}
