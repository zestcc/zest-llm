package cn.zest.www.zestllm.plugin.gateway.litellm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.litellm")
public class LiteLLMProperties {
    private String baseUrl = "http://127.0.0.1:4000";
    private String apiKey = "";
    /** 榛樿 openai锛沘nthropic 鏃惰蛋 LiteLLM /v1/messages */
    private String defaultApiProtocol = "openai";
    private long connectTimeoutMs = 5000;
    private long readTimeoutMs = 120000;
}

