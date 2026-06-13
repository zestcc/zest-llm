package cn.zest.www.zestllm.admin.service.sso;

import cn.zest.www.zestllm.admin.model.request.AdminOidcCallbackRequest;
import cn.zest.www.zestllm.admin.model.request.AdminOidcExchangeRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcAuthorizeVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcConfigVO;
import cn.zest.www.zestllm.admin.service.sso.spi.AdminSsoProviderRegistry;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoAuthorizeInfo;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoCallbackInput;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoLoginResult;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProvider;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoPublicConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Admin SSO 认证门面 — 委托 {@link AdminSsoProviderRegistry} 选择具体提供方。
 */
@Service
@RequiredArgsConstructor
public class AdminSsoAuthService {

    private final AdminSsoProviderRegistry providerRegistry;
    private final AdminSsoUserProvisioner userProvisioner;

    public AdminOidcConfigVO getPublicConfig() {
        AdminSsoPublicConfig cfg = providerRegistry.resolve().buildPublicConfig(providerRegistry.properties());
        AdminOidcConfigVO vo = new AdminOidcConfigVO();
        vo.setEnabled(cfg.enabled());
        vo.setProvider(cfg.provider());
        vo.setDisplayName(cfg.displayName());
        vo.setClientId(cfg.clientId());
        vo.setIssuer(cfg.issuer());
        return vo;
    }

    public AdminOidcAuthorizeVO buildAuthorizeUrl() {
        AdminSsoAuthorizeInfo info = providerRegistry.resolve().buildAuthorizeUrl(providerRegistry.properties());
        AdminOidcAuthorizeVO vo = new AdminOidcAuthorizeVO();
        vo.setAuthorizationUrl(info.authorizationUrl());
        vo.setState(info.state());
        return vo;
    }

    public String buildLogoutUrl() {
        if (!providerRegistry.properties().isEnabled()) {
            return null;
        }
        return providerRegistry.resolve().buildLogoutUrl(providerRegistry.properties());
    }

    public AdminLoginVO handleCallback(AdminOidcCallbackRequest request) {
        AdminSsoLoginResult result = providerRegistry.resolve().handleCallback(
                new AdminSsoCallbackInput(request.getCode(), request.getState()),
                providerRegistry.properties());
        return userProvisioner.toLoginVo(result);
    }

    public AdminLoginVO exchangeIdToken(AdminOidcExchangeRequest request) {
        AdminSsoProvider provider = providerRegistry.resolve();
        AdminSsoLoginResult result = provider.exchangeIdToken(request.getIdToken(), providerRegistry.properties());
        return userProvisioner.toLoginVo(result);
    }
}
