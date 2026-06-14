package cn.zest.www.zestllm.infra;

import cn.zest.www.zestllm.infra.config.LlmPluginProperties;
import cn.zest.www.zestllm.infra.config.VaultProperties;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.gateway.SseStreamHandler;
import cn.zest.www.zestllm.infra.secret.CompositeSecretResolver;
import cn.zest.www.zestllm.infra.secret.EnvSecretResolver;
import cn.zest.www.zestllm.infra.secret.VaultSecretResolver;
import cn.zest.www.zestllm.infra.tool.FunctionCallLoop;
import cn.zest.www.zestllm.infra.tool.ToolOrchestrator;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterLoader;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
@EnableConfigurationProperties({LlmAdapterProperties.class,
        VaultProperties.class,
        LlmPluginProperties.class})
public class LlmInfraAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean(SecretResolver.class)
    public SecretResolver compositeSecretResolver(VaultProperties vaultProperties,
                                                  ObjectMapper objectMapper,
                                                  RestClient.Builder restClientBuilder) {
        return new CompositeSecretResolver(List.of(
                new EnvSecretResolver(),
                new VaultSecretResolver(vaultProperties, objectMapper, restClientBuilder)
        ));
    }

    @Bean
    @ConditionalOnBean(McpToolAdapter.class)
    @ConditionalOnMissingBean
    public ToolOrchestrator toolOrchestrator(McpToolAdapter mcpToolAdapter,
                                             SecretResolver secretResolver,
                                             ObjectMapper objectMapper) {
        return new ToolOrchestrator(mcpToolAdapter, secretResolver, objectMapper);
    }

    @Bean
    @ConditionalOnBean(ToolOrchestrator.class)
    @ConditionalOnMissingBean
    public FunctionCallLoop functionCallLoop(ToolOrchestrator toolOrchestrator, ObjectMapper objectMapper) {
        return new FunctionCallLoop(toolOrchestrator, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalAdapterRegistry externalAdapterRegistry() {
        return new ExternalAdapterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalAdapterLoader externalAdapterLoader(LlmPluginProperties pluginProperties,
                                                       ExternalAdapterRegistry registry) {
        return new ExternalAdapterLoader(pluginProperties, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public SseStreamHandler sseStreamHandler(ObjectMapper objectMapper) {
        return new SseStreamHandler(objectMapper);
    }
}
