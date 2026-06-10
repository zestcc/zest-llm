package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.ragflow")
public class RagflowProperties {
    private String baseUrl = "http://localhost:9380";
    private String apiKey = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
}
