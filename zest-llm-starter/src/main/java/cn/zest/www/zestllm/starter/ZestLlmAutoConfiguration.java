package cn.zest.www.zestllm.starter;

import cn.zest.www.zestllm.agent.LlmAgentClient;
import cn.zest.www.zestllm.starter.aop.ZestLlmAspect;
import cn.zest.www.zestllm.starter.client.LlmControlPlaneClient;
import cn.zest.www.zestllm.starter.config.ZestLlmProperties;
import cn.zest.www.zestllm.starter.mapper.AiResultMapper;
import cn.zest.www.zestllm.starter.registry.MethodRegistryScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;

@AutoConfiguration
@ConditionalOnProperty(name = "zest.llm.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZestLlmProperties.class)
public class ZestLlmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper zestLlmObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public LlmControlPlaneClient llmControlPlaneClient(ZestLlmProperties properties) {
        return new LlmControlPlaneClient(properties);
    }

    @Bean
    public AiResultMapper aiResultMapper(ObjectMapper objectMapper) {
        return new AiResultMapper(objectMapper);
    }

    @Bean
    public ZestLlmAspect zestLlmAspect(LlmControlPlaneClient controlPlaneClient,
                                       ObjectProvider<LlmAgentClient> agentClientProvider,
                                       AiResultMapper aiResultMapper,
                                       ZestLlmProperties properties) {
        return new ZestLlmAspect(controlPlaneClient, agentClientProvider, aiResultMapper, properties);
    }

    @Bean
    public MethodRegistryScanner methodRegistryScanner(ApplicationContext applicationContext,
                                                       LlmControlPlaneClient controlPlaneClient,
                                                       ZestLlmProperties properties) {
        return new MethodRegistryScanner(applicationContext, controlPlaneClient, properties);
    }
}
