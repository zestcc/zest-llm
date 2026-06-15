package cn.zest.www.zestllm.plugin.identity.zestsso;

import cn.zest.sso.client.ZestSsoClientProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public class ZestSsoPropertyBridge {

    private final ZestSsoPluginProperties ssoProperties;
    private final ZestSsoClientProperties zestSsoClientProperties;

    @PostConstruct
    public void sync() {
        zestSsoClientProperties.setEnabled(true);
        zestSsoClientProperties.setIssuer(ssoProperties.getIssuer());
        zestSsoClientProperties.setClientId(ssoProperties.getClientId());
        zestSsoClientProperties.setClientSecret(ssoProperties.getClientSecret());
        zestSsoClientProperties.setRedirectUri(ssoProperties.getRedirectUri());
        zestSsoClientProperties.setScopes(ssoProperties.getScopes());
        zestSsoClientProperties.setBackchannelLogoutPath("/api/admin/auth/sso/backchannel-logout");
    }
}
