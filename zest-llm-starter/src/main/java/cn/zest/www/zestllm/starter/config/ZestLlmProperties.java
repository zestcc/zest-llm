package cn.zest.www.zestllm.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm")
public class ZestLlmProperties {

    private boolean enabled = true;

    private String controlPlaneUrl = "http://localhost:8088";

    private String appKey;

    private String authToken;

    private boolean registryOnStartup = true;

    /** invoke: 合一调用；agent: prepare → execute → report */
    private String runtimeMode = "invoke";
}
