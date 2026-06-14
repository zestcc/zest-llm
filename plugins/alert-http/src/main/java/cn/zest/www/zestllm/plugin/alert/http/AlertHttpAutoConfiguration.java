package cn.zest.www.zestllm.plugin.alert.http;

import cn.zest.www.zestllm.spi.alert.AlertWebhookAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class AlertHttpAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.alert-webhook", havingValue = "http", matchIfMissing = true)
    @ConditionalOnMissingBean(AlertWebhookAdapter.class)
    public AlertWebhookAdapter httpAlertWebhookAdapter(RestClient.Builder restClientBuilder,
                                                       ObjectMapper objectMapper) {
        return new HttpAlertWebhookAdapter(restClientBuilder.build(), objectMapper);
    }
}
