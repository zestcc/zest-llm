package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.litellm")
public class LiteLLMProperties {
    private String baseUrl = "http://127.0.0.1:4000";
    private String apiKey = "";
    private long connectTimeoutMs = 5000;
    private long readTimeoutMs = 120000;
}
