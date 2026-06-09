package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.langfuse")
public class LangfuseProperties {
    private String baseUrl = "http://localhost:3000";
    private String publicKey = "";
    private String secretKey = "";
    private boolean enabled = true;
}
