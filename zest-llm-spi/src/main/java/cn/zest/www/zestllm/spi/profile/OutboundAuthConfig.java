package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class OutboundAuthConfig {
    /** API_KEY_REF | OIDC_CLIENT_CREDENTIALS | NONE */
    private String mode = "API_KEY_REF";
    private String secretRef;
    private Map<String, Object> extra = new LinkedHashMap<>();
}
