package cn.zest.www.zestllm.admin.service.sso.provider;

import cn.zest.www.zestllm.spi.adminsso.AdminSsoProvider;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProviderConfig;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoAuthorizeInfo;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoCallbackInput;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoLoginResult;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoPublicConfig;
import cn.zest.www.zestllm.admin.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * SSO 关闭时的空实现。
 */
@Component
public class DisabledAdminSsoProvider implements AdminSsoProvider {

    @Override
    public String providerId() {
        return "none";
    }

    @Override
    public AdminSsoPublicConfig buildPublicConfig(AdminSsoProviderConfig config) {
        return new AdminSsoPublicConfig(false, "none", "SSO", null, null);
    }

    @Override
    public AdminSsoAuthorizeInfo buildAuthorizeUrl(AdminSsoProviderConfig config) {
        throw BusinessException.badRequest("SSO 未启用");
    }

    @Override
    public String buildLogoutUrl(AdminSsoProviderConfig config) {
        return null;
    }

    @Override
    public AdminSsoLoginResult handleCallback(AdminSsoCallbackInput input, AdminSsoProviderConfig config) {
        throw BusinessException.badRequest("SSO 未启用");
    }
}
