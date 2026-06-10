package cn.zest.www.zestllm.admin.service.auth;

import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthContext;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OidcJwtAuthStrategy implements RuntimeAuthStrategy {

    public static final String MODE = "OIDC_JWT";

    private final OidcJwtValidator oidcJwtValidator;

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public void authenticate(RuntimeAuthContext context) {
        InboundAuthConfig config = context.getInboundAuth();
        if (config == null || context.getBearerToken() == null || context.getBearerToken().isBlank()) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        oidcJwtValidator.validate(context.getBearerToken(), config);
    }
}
