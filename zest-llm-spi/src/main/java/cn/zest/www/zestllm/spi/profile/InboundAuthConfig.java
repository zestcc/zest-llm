package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class InboundAuthConfig {
    /** STATIC_TOKEN | OIDC_JWT | API_KEY */
    private String mode = "STATIC_TOKEN";
    private String issuer;
    private String audience;
    private String jwksUri;
    private Map<String, Object> extra = new LinkedHashMap<>();
}
