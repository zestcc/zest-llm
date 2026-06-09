package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest-llm.admin.jwt")
public class JwtProperties {
    private String secret = "change-me-in-production-zest-llm-admin-jwt-secret-key";
    private long expirationMs = 86400000L;
}
