package cn.zest.www.zestllm.spi.auth;

import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuntimeAuthContext {
    private String appKey;
    private String bearerToken;
    private String tokenHash;
    private InboundAuthConfig inboundAuth;
}
