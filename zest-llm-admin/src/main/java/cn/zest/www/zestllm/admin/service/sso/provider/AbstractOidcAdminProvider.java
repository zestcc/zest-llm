package cn.zest.www.zestllm.admin.service.sso.provider;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.service.auth.OidcJwtValidator;
import cn.zest.www.zestllm.admin.service.sso.AdminSsoUserProvisioner;
import cn.zest.www.zestllm.admin.service.sso.oidc.OidcEndpointResolver;
import cn.zest.www.zestllm.admin.service.sso.oidc.OidcEndpoints;
import cn.zest.www.zestllm.admin.service.sso.oidc.OidcTokenClient;
import cn.zest.www.zestllm.admin.service.sso.oidc.PkceUtils;
import cn.zest.www.zestllm.admin.service.sso.store.AdminSsoPkceStore;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoAuthorizeInfo;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoCallbackInput;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoLoginResult;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProvider;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProviderConfig;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoPublicConfig;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * OIDC + PKCE 通用流程基类。
 */
@RequiredArgsConstructor
public abstract class AbstractOidcAdminProvider implements AdminSsoProvider {

    protected final AdminSsoPkceStore pkceStore;
    protected final OidcEndpointResolver endpointResolver;
    protected final OidcTokenClient tokenClient;
    protected final OidcJwtValidator jwtValidator;
    protected final AdminSsoUserProvisioner userProvisioner;

    protected abstract String displayName();

    @Override
    public AdminSsoPublicConfig buildPublicConfig(AdminSsoProviderConfig config) {
        return new AdminSsoPublicConfig(
                config.isEnabled(),
                providerId(),
                displayName(),
                config.getIssuer(),
                config.getClientId()
        );
    }

    @Override
    public AdminSsoAuthorizeInfo buildAuthorizeUrl(AdminSsoProviderConfig config) {
        ensureEnabled(config);
        OidcEndpoints endpoints = endpointResolver.resolve(config);
        String state = PkceUtils.randomBase64Url(16);
        String codeVerifier = PkceUtils.randomBase64Url(32);
        String codeChallenge = PkceUtils.sha256Base64Url(codeVerifier);
        pkceStore.save(state, codeVerifier);

        String scopes = String.join(" ", config.getScopes());
        String url = endpoints.authorizationEndpoint()
                + "?response_type=code"
                + "&client_id=" + PkceUtils.urlEncode(config.getClientId())
                + "&redirect_uri=" + PkceUtils.urlEncode(config.getRedirectUri())
                + "&scope=" + PkceUtils.urlEncode(scopes)
                + "&state=" + PkceUtils.urlEncode(state)
                + "&code_challenge=" + PkceUtils.urlEncode(codeChallenge)
                + "&code_challenge_method=S256";

        return new AdminSsoAuthorizeInfo(url, state);
    }

    @Override
    public String buildLogoutUrl(AdminSsoProviderConfig config) {
        OidcEndpoints endpoints = endpointResolver.resolve(config);
        String redirect = config.getPostLogoutRedirectUri();
        if (StringUtils.hasText(endpoints.endSessionEndpoint())) {
            return endpoints.endSessionEndpoint()
                    + "?post_logout_redirect_uri=" + PkceUtils.urlEncode(redirect);
        }
        return config.getIssuer().replaceAll("/$", "")
                + "/connect/logout?post_logout_redirect_uri=" + PkceUtils.urlEncode(redirect);
    }

    @Override
    public AdminSsoLoginResult handleCallback(AdminSsoCallbackInput input, AdminSsoProviderConfig config) {
        ensureEnabled(config);
        String codeVerifier = pkceStore.consume(input.state());
        if (!StringUtils.hasText(codeVerifier)) {
            throw BusinessException.unauthorized("无效的 state 或已过期");
        }
        OidcEndpoints endpoints = endpointResolver.resolve(config);
        String idToken = tokenClient.exchangeCodeForIdToken(input.code(), codeVerifier, endpoints, config);
        Claims claims = validateIdToken(idToken, endpoints, config);
        return userProvisioner.provisionFromClaims(providerId(), claims, config);
    }

    @Override
    public AdminSsoLoginResult exchangeIdToken(String idToken, AdminSsoProviderConfig config) {
        ensureEnabled(config);
        if (!StringUtils.hasText(idToken)) {
            throw BusinessException.badRequest("idToken 不能为空");
        }
        OidcEndpoints endpoints = endpointResolver.resolve(config);
        Claims claims = validateIdToken(idToken, endpoints, config);
        return userProvisioner.provisionFromClaims(providerId(), claims, config);
    }

    protected Claims validateIdToken(String idToken, OidcEndpoints endpoints, AdminSsoProviderConfig config) {
        InboundAuthConfig authConfig = new InboundAuthConfig();
        authConfig.setMode("OIDC_JWT");
        authConfig.setIssuer(endpoints.issuer());
        authConfig.setAudience(config.getAudience());
        authConfig.setJwksUri(endpoints.jwksUri());
        return jwtValidator.parseAndValidate(idToken, authConfig);
    }

    protected void ensureEnabled(AdminSsoProviderConfig config) {
        if (!config.isEnabled()) {
            throw BusinessException.badRequest("Admin SSO 未启用");
        }
    }
}
