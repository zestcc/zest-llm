package cn.zest.www.zestllm.plugin.knowledge.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.http-knowledge")
public class HttpKnowledgeProperties {
    private String baseUrl = "http://localhost:8090";
    private String apiKey = "";
    private String retrievePath = "/v1/retrieve";
    private String healthPath = "/health";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
}

