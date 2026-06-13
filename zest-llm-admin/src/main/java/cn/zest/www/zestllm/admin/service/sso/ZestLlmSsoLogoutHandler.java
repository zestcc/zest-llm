package cn.zest.www.zestllm.admin.service.sso;

import cn.zest.sso.client.SsoLogoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zest-llm.admin.sso", name = "enabled", havingValue = "true")
public class ZestLlmSsoLogoutHandler implements SsoLogoutHandler {

    private final AdminSessionRevocationService revocationService;

    @Override
    public void onBackchannelLogout(String principal) {
        revocationService.revokeByUsername(principal);
    }
}
