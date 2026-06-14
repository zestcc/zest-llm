package cn.zest.www.zestllm.infra;

import cn.zest.www.zestllm.infra.alert.HttpAlertWebhookAdapter;
import cn.zest.www.zestllm.infra.alert.NoopAlertWebhookAdapter;
import cn.zest.www.zestllm.infra.audit.NoopAuditAdapter;
import cn.zest.www.zestllm.infra.cache.CaffeinePolicyCacheAdapter;
import cn.zest.www.zestllm.infra.cache.NoopResponseCacheAdapter;
import cn.zest.www.zestllm.infra.cache.ValkeyPolicyCacheAdapter;
import cn.zest.www.zestllm.infra.cache.ValkeyResponseCacheAdapter;
import cn.zest.www.zestllm.infra.config.LlmPluginProperties;
import cn.zest.www.zestllm.infra.config.VaultProperties;
import cn.zest.www.zestllm.infra.config.KafkaReportProperties;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.gateway.SseStreamHandler;
import cn.zest.www.zestllm.infra.knowledge.NoopKnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.infra.learning.NoopLearningPipelineAdapter;
import cn.zest.www.zestllm.infra.runtime.NoopAgentRuntimeAdapter;
import cn.zest.www.zestllm.infra.secret.CompositeSecretResolver;
import cn.zest.www.zestllm.infra.secret.EnvSecretResolver;
import cn.zest.www.zestllm.infra.secret.VaultSecretResolver;
import cn.zest.www.zestllm.infra.tool.FunctionCallLoop;
import cn.zest.www.zestllm.infra.tool.HttpMcpToolAdapter;
import cn.zest.www.zestllm.infra.tool.ToolOrchestrator;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.infra.observability.NoopObservabilityAdapter;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterLoader;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.infra.prompt.HandlebarsPromptRenderer;
import cn.zest.www.zestllm.infra.quota.NoopQuotaAdapter;
import cn.zest.www.zestllm.infra.guardrails.KeywordBlocklistModerationAdapter;
import cn.zest.www.zestllm.infra.guardrails.NoopContentModerationAdapter;
import cn.zest.www.zestllm.infra.schema.JsonOutputSchemaValidator;
import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.spi.alert.AlertWebhookAdapter;
import cn.zest.www.zestllm.spi.audit.AuditAdapter;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;
import cn.zest.www.zestllm.infra.report.KafkaReportChannelAdapter;
import cn.zest.www.zestllm.infra.report.SyncReportChannelAdapter;
import cn.zest.www.zestllm.spi.report.ReportChannelAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@Configuration
@EnableConfigurationProperties({LlmAdapterProperties.class,
        VaultProperties.class, KafkaReportProperties.class,
        LlmPluginProperties.class})
public class LlmInfraAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.observability", havingValue = "noop", matchIfMissing = true)
    public ObservabilityAdapter noopObservabilityAdapter() {
        return new NoopObservabilityAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.agent-runtime", havingValue = "noop")
    public AgentRuntimeAdapter noopAgentRuntimeAdapter() {
        return new NoopAgentRuntimeAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.knowledge-retrieval", havingValue = "noop", matchIfMissing = true)
    public KnowledgeRetrievalAdapter noopKnowledgeRetrievalAdapter() {
        return new NoopKnowledgeRetrievalAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.learning-pipeline", havingValue = "noop", matchIfMissing = true)
    public LearningPipelineAdapter noopLearningPipelineAdapter() {
        return new NoopLearningPipelineAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.prompt-renderer", havingValue = "handlebars", matchIfMissing = true)
    @ConditionalOnMissingBean(PromptRenderer.class)
    public PromptRenderer handlebarsPromptRenderer() {
        return new HandlebarsPromptRenderer();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.response-cache", havingValue = "valkey")
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnMissingBean(ResponseCacheAdapter.class)
    public ResponseCacheAdapter valkeyResponseCacheAdapter(StringRedisTemplate redisTemplate,
                                                           ObjectMapper objectMapper) {
        return new ValkeyResponseCacheAdapter(redisTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.response-cache", havingValue = "noop", matchIfMissing = true)
    @ConditionalOnMissingBean(ResponseCacheAdapter.class)
    public ResponseCacheAdapter noopResponseCacheAdapter() {
        return new NoopResponseCacheAdapter();
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
    @ConditionalOnProperty(name = "zest.llm.adapters.content-moderation", havingValue = "keyword-blocklist", matchIfMissing = true)
    @ConditionalOnMissingBean(ContentModerationAdapter.class)
    public ContentModerationAdapter keywordBlocklistModerationAdapter() {
        return new KeywordBlocklistModerationAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.content-moderation", havingValue = "noop")
    @ConditionalOnMissingBean(ContentModerationAdapter.class)
    public ContentModerationAdapter noopContentModerationAdapter() {
        return new NoopContentModerationAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.report-channel", havingValue = "sync", matchIfMissing = true)
    @ConditionalOnMissingBean(ReportChannelAdapter.class)
    public ReportChannelAdapter syncReportChannelAdapter() {
        return new SyncReportChannelAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.report-channel", havingValue = "kafka")
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnMissingBean(ReportChannelAdapter.class)
    public ReportChannelAdapter kafkaReportChannelAdapter(org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate,
                                                          KafkaReportProperties properties,
                                                          ObjectMapper objectMapper) {
        return new KafkaReportChannelAdapter(kafkaTemplate, properties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.alert-webhook", havingValue = "http", matchIfMissing = true)
    @ConditionalOnMissingBean(AlertWebhookAdapter.class)
    public AlertWebhookAdapter httpAlertWebhookAdapter(RestClient.Builder restClientBuilder,
                                                       ObjectMapper objectMapper) {
        return new HttpAlertWebhookAdapter(restClientBuilder.build(), objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.alert-webhook", havingValue = "noop")
    @ConditionalOnMissingBean(AlertWebhookAdapter.class)
    public AlertWebhookAdapter noopAlertWebhookAdapter() {
        return new NoopAlertWebhookAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.audit", havingValue = "noop", matchIfMissing = true)
    @ConditionalOnMissingBean(AuditAdapter.class)
    public AuditAdapter noopAuditAdapter() {
        return new NoopAuditAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(McpToolAdapter.class)
    public McpToolAdapter httpMcpToolAdapter(ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        return new HttpMcpToolAdapter(objectMapper, restClientBuilder);
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
    @ConditionalOnMissingBean
    public ToolOrchestrator toolOrchestrator(McpToolAdapter mcpToolAdapter,
                                             SecretResolver secretResolver,
                                             ObjectMapper objectMapper) {
        return new ToolOrchestrator(mcpToolAdapter, secretResolver, objectMapper);
    }

    @Bean
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
