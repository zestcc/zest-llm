package cn.zest.www.zestllm.admin.service.sso.provider;

import cn.zest.www.zestllm.admin.config.AdminSsoProperties;
import cn.zest.www.zestllm.admin.service.auth.OidcJwtValidator;
import cn.zest.www.zestllm.admin.service.sso.AdminSsoUserProvisioner;
import cn.zest.www.zestllm.admin.service.sso.oidc.OidcEndpointResolver;
import cn.zest.www.zestllm.admin.service.sso.oidc.OidcTokenClient;
import cn.zest.www.zestllm.admin.service.sso.store.AdminSsoPkceStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通用 OIDC 提供方 — 适用于 Keycloak、Authing 等标准 IdP。
 */
@Component
public class GenericOidcAdminProvider extends AbstractOidcAdminProvider {

    public static final String PROVIDER_ID = "oidc";

    private final AdminSsoProperties ssoProperties;

    public GenericOidcAdminProvider(AdminSsoPkceStore pkceStore,
                                    OidcEndpointResolver endpointResolver,
                                    OidcTokenClient tokenClient,
                                    OidcJwtValidator jwtValidator,
                                    AdminSsoUserProvisioner userProvisioner,
                                    AdminSsoProperties ssoProperties) {
        super(pkceStore, endpointResolver, tokenClient, jwtValidator, userProvisioner);
        this.ssoProperties = ssoProperties;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    protected String displayName() {
        return StringUtils.hasText(ssoProperties.getDisplayName()) ? ssoProperties.getDisplayName() : "SSO";
    }
}
