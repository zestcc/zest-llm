package cn.zest.www.zestllm.admin.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 向后兼容：当 {@code zest-llm.admin.sso.client-id} 未配置时，从 legacy {@code zest-llm.admin.oidc.*} 合并。
 */
@Component
@RequiredArgsConstructor
public class AdminSsoLegacyConfiguration {

    private final AdminSsoProperties ssoProperties;
    private final AdminOidcProperties oidcProperties;

    @PostConstruct
    public void mergeLegacyOidcConfig() {
        if (StringUtils.hasText(ssoProperties.getClientId())) {
            return;
        }
        if (!StringUtils.hasText(oidcProperties.getClientId())) {
            return;
        }
        ssoProperties.setEnabled(oidcProperties.isEnabled());
        if (StringUtils.hasText(oidcProperties.getIssuer())) {
            ssoProperties.setIssuer(oidcProperties.getIssuer());
        }
        if (StringUtils.hasText(oidcProperties.getAudience())) {
            ssoProperties.setAudience(oidcProperties.getAudience());
        }
        if (StringUtils.hasText(oidcProperties.getJwksUri())) {
            ssoProperties.setJwksUri(oidcProperties.getJwksUri());
        }
        ssoProperties.setClientId(oidcProperties.getClientId());
        if (StringUtils.hasText(oidcProperties.getClientSecret())) {
            ssoProperties.setClientSecret(oidcProperties.getClientSecret());
        }
        if (StringUtils.hasText(oidcProperties.getRedirectUri())) {
            ssoProperties.setRedirectUri(oidcProperties.getRedirectUri());
        }
        if (StringUtils.hasText(oidcProperties.getPostLogoutRedirectUri())) {
            ssoProperties.setPostLogoutRedirectUri(oidcProperties.getPostLogoutRedirectUri());
        }
        if (oidcProperties.getScopes() != null && !oidcProperties.getScopes().isEmpty()) {
            ssoProperties.setScopes(oidcProperties.getScopes());
        }
        AdminSsoProperties.SsoClaimsProperties claims = ssoProperties.getClaims();
        if (StringUtils.hasText(oidcProperties.getUsernameClaim())) {
            claims.setUsernameClaim(oidcProperties.getUsernameClaim());
        }
        if (StringUtils.hasText(oidcProperties.getRolesClaim())) {
            claims.setRolesClaim(oidcProperties.getRolesClaim());
        }
        if (StringUtils.hasText(oidcProperties.getDefaultRole())) {
            claims.setDefaultRole(oidcProperties.getDefaultRole());
        }
    }
}
