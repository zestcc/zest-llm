package cn.zest.www.zestllm.flow;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@AutoConfiguration
@ConditionalOnProperty(name = "zest.llm.flow.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZestLlmFlowProperties.class)
public class ZestLlmFlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "zestLlmFlowRestClient")
    public RestClient zestLlmFlowRestClient(ZestLlmFlowProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(120));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getControlPlaneUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (properties.getAuthToken() != null && !properties.getAuthToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getAuthToken());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ZestLlmFlowNodeExecutor zestLlmFlowNodeExecutor(RestClient zestLlmFlowRestClient,
                                                           ZestLlmFlowProperties properties) {
        return new ZestLlmFlowNodeExecutor(zestLlmFlowRestClient, properties);
    }
}
