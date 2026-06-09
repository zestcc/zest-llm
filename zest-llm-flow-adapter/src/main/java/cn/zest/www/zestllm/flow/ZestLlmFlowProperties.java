package cn.zest.www.zestllm.flow;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.flow")
public class ZestLlmFlowProperties {

    private boolean enabled = true;

    private String controlPlaneUrl = "http://localhost:8088";

    private String appKey;

    private String authToken;
}
