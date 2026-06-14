package cn.zest.www.zestllm.plugin.gateway.litellm;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(LiteLLMProperties.class)
public class GatewayLiteLLMAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.model-gateway", havingValue = "litellm", matchIfMissing = true)
    public ModelGatewayAdapter liteLLMGatewayAdapter(LiteLLMProperties properties, ObjectMapper objectMapper) {
        return new LiteLLMGatewayAdapter(properties, objectMapper);
    }
}
