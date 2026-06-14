package cn.zest.www.zestllm.plugin.noop;

import cn.zest.www.zestllm.spi.alert.AlertWebhookAdapter;
import cn.zest.www.zestllm.spi.audit.AuditAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class NoopAdaptersAutoConfiguration {

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
    @ConditionalOnProperty(name = "zest.llm.adapters.response-cache", havingValue = "noop", matchIfMissing = true)
    @ConditionalOnMissingBean(ResponseCacheAdapter.class)
    public ResponseCacheAdapter noopResponseCacheAdapter() {
        return new NoopResponseCacheAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.quota", havingValue = "noop", matchIfMissing = true)
    @ConditionalOnMissingBean(QuotaAdapter.class)
    public QuotaAdapter noopQuotaAdapter() {
        return new NoopQuotaAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.content-moderation", havingValue = "noop")
    @ConditionalOnMissingBean(ContentModerationAdapter.class)
    public ContentModerationAdapter noopContentModerationAdapter() {
        return new NoopContentModerationAdapter();
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
}
