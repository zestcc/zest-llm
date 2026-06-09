package cn.zest.www.zestllm.infra;

import cn.zest.www.zestllm.infra.audit.NoopAuditAdapter;
import cn.zest.www.zestllm.infra.cache.CaffeinePolicyCacheAdapter;
import cn.zest.www.zestllm.infra.cache.ValkeyPolicyCacheAdapter;
import cn.zest.www.zestllm.infra.config.LangfuseProperties;
import cn.zest.www.zestllm.infra.config.LiteLLMProperties;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.gateway.LiteLLMGatewayAdapter;
import cn.zest.www.zestllm.infra.observability.LangfuseObservabilityAdapter;
import cn.zest.www.zestllm.infra.observability.NoopObservabilityAdapter;
import cn.zest.www.zestllm.infra.prompt.HandlebarsPromptRenderer;
import cn.zest.www.zestllm.infra.quota.NoopQuotaAdapter;
import cn.zest.www.zestllm.infra.schema.JsonOutputSchemaValidator;
import cn.zest.www.zestllm.spi.audit.AuditAdapter;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({LiteLLMProperties.class, LlmAdapterProperties.class, LangfuseProperties.class})
public class LlmInfraAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.model-gateway", havingValue = "litellm", matchIfMissing = true)
    @ConditionalOnMissingBean(ModelGatewayAdapter.class)
    public ModelGatewayAdapter liteLLMGatewayAdapter(LiteLLMProperties properties, ObjectMapper objectMapper) {
        return new LiteLLMGatewayAdapter(properties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.observability", havingValue = "langfuse")
    @ConditionalOnMissingBean(ObservabilityAdapter.class)
    public ObservabilityAdapter langfuseObservabilityAdapter(LangfuseProperties properties,
                                                             ObjectMapper objectMapper) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        return new LangfuseObservabilityAdapter(properties, objectMapper, client);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.observability", havingValue = "noop", matchIfMissing = true)
    @ConditionalOnMissingBean(ObservabilityAdapter.class)
    public ObservabilityAdapter noopObservabilityAdapter() {
        return new NoopObservabilityAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.prompt-renderer", havingValue = "handlebars", matchIfMissing = true)
    @ConditionalOnMissingBean(PromptRenderer.class)
    public PromptRenderer handlebarsPromptRenderer() {
        return new HandlebarsPromptRenderer();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.policy-cache", havingValue = "valkey")
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnMissingBean(PolicyCacheAdapter.class)
    public PolicyCacheAdapter valkeyPolicyCacheAdapter(StringRedisTemplate redisTemplate,
                                                       ObjectMapper objectMapper) {
        return new ValkeyPolicyCacheAdapter(redisTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.policy-cache", havingValue = "caffeine", matchIfMissing = true)
    @ConditionalOnMissingBean(PolicyCacheAdapter.class)
    public PolicyCacheAdapter caffeinePolicyCacheAdapter() {
        return new CaffeinePolicyCacheAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.quota", havingValue = "noop", matchIfMissing = true)
    @ConditionalOnMissingBean(QuotaAdapter.class)
    public QuotaAdapter noopQuotaAdapter() {
        return new NoopQuotaAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.output-schema-validator", havingValue = "json", matchIfMissing = true)
    @ConditionalOnMissingBean(OutputSchemaValidator.class)
    public OutputSchemaValidator jsonOutputSchemaValidator(ObjectMapper objectMapper) {
        return new JsonOutputSchemaValidator(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.audit", havingValue = "noop", matchIfMissing = true)
    @ConditionalOnMissingBean(AuditAdapter.class)
    public AuditAdapter noopAuditAdapter() {
        return new NoopAuditAdapter();
    }
}
