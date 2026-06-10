package cn.zest.www.zestllm.admin.service.auth;

import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthContext;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthStrategy;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthStrategy implements RuntimeAuthStrategy {

    public static final String MODE = "API_KEY";

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public void authenticate(RuntimeAuthContext context) {
        InboundAuthConfig config = context.getInboundAuth();
        if (config == null || config.getExtra() == null) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        Object expectedHash = config.getExtra().get("apiKeyHash");
        if (expectedHash == null || context.getBearerToken() == null) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        if (!TokenHashUtil.matches(context.getBearerToken(), expectedHash.toString())) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
    }
}
