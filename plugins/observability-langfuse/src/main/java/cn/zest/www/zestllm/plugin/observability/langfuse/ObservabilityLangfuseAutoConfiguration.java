package cn.zest.www.zestllm.plugin.observability.langfuse;

import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@EnableConfigurationProperties(LangfuseProperties.class)
public class ObservabilityLangfuseAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.observability", havingValue = "langfuse")
    public ObservabilityAdapter langfuseObservabilityAdapter(LangfuseProperties properties,
                                                             ObjectMapper objectMapper) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        return new LangfuseObservabilityAdapter(properties, objectMapper, client);
    }
}
