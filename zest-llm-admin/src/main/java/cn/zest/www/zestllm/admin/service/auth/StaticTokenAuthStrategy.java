package cn.zest.www.zestllm.admin.service.auth;

import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthContext;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthStrategy;
import org.springframework.stereotype.Component;

@Component
public class StaticTokenAuthStrategy implements RuntimeAuthStrategy {

    public static final String MODE = "STATIC_TOKEN";

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public void authenticate(RuntimeAuthContext context) {
        if (context.getBearerToken() == null || context.getBearerToken().isBlank()) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        if (context.getTokenHash() == null || !TokenHashUtil.matches(context.getBearerToken(), context.getTokenHash())) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
    }
}
