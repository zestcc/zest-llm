package cn.zest.www.zestllm.admin.config;

import cn.zest.www.zestllm.plugin.gateway.litellm.LiteLLMProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

/**
 * local profile：将 application-local.yml 中的 api-key 同步为 LITELLM_API_KEY，
 * 供 Profile outboundAuth secretRef=env:LITELLM_API_KEY 解析。
 */
@Component
@Profile("local")
public class LocalLiteLLMSecretBootstrap {

    private final LiteLLMProperties liteLLMProperties;

    public LocalLiteLLMSecretBootstrap(LiteLLMProperties liteLLMProperties) {
        this.liteLLMProperties = liteLLMProperties;
    }

    @PostConstruct
    public void syncGatewayApiKey() {
        String apiKey = liteLLMProperties.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            return;
        }
        if (!StringUtils.hasText(System.getProperty("LITELLM_API_KEY"))) {
            System.setProperty("LITELLM_API_KEY", apiKey);
        }
        // EnvSecretResolver 优先读 getenv；本地 IDE 无法改环境变量，property 作回退
    }
}
