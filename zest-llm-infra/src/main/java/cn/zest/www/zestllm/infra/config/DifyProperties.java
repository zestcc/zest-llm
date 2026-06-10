package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.dify")
public class DifyProperties {
    private String baseUrl = "http://localhost:5001";
    private String apiKey = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 60000;
}
