package cn.zest.www.zestllm.plugin.identity.zestsso;

import cn.zest.sso.client.SsoLogoutHandler;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoSessionRevocation;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ZestSsoLogoutHandler implements SsoLogoutHandler {

    private final AdminSsoSessionRevocation sessionRevocation;

    @Override
    public void onBackchannelLogout(String principal) {
        sessionRevocation.revokeByUsername(principal);
    }
}
