package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest-llm.admin.integration")
public class IntegrationWebhookProperties {
    /** Optional webhook URL on profile publish success/fail */
    private String webhookUrl = "";
}
