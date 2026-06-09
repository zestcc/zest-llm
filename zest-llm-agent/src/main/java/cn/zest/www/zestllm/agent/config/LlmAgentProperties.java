package cn.zest.www.zestllm.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "zest.llm.agent")
public class LlmAgentProperties {

    private boolean enabled = false;

    private Duration cacheTtl = Duration.ofSeconds(300);

    private String controlPlaneUrl = "http://localhost:8088";

    private String appKey;

    private String authToken;

    private String litellmUrl = "http://localhost:4000";

    private String litellmApiKey;
}
