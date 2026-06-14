package cn.zest.www.zestllm.infra.config;

import cn.zest.www.zestllm.infra.gateway.RoutingModelGatewayAdapter;
import cn.zest.www.zestllm.infra.knowledge.RoutingKnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.infra.learning.RoutingLearningPipelineAdapter;
import cn.zest.www.zestllm.infra.observability.RoutingObservabilityAdapter;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.infra.runtime.RoutingAgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@AutoConfiguration(afterName = "cn.zest.www.zestllm.infra.LlmInfraAutoConfiguration")
public class LlmRoutingAutoConfiguration {

    @Bean
    @Primary
    public ModelGatewayAdapter routingModelGatewayAdapter(LlmAdapterProperties adapterProperties,
                                                            ExternalAdapterRegistry externalAdapterRegistry,
                                                            ObjectProvider<ModelGatewayAdapter> adapterProvider) {
        List<ModelGatewayAdapter> builtIn = adapterProvider.stream()
                .filter(item -> !(item instanceof RoutingModelGatewayAdapter))
                .toList();
        return new RoutingModelGatewayAdapter(adapterProperties, externalAdapterRegistry, builtIn);
    }

    @Bean
    @Primary
    public ObservabilityAdapter routingObservabilityAdapter(LlmAdapterProperties adapterProperties,
                                                              ExternalAdapterRegistry externalAdapterRegistry,
                                                              ObjectProvider<ObservabilityAdapter> adapterProvider) {
        List<ObservabilityAdapter> builtIn = adapterProvider.stream()
                .filter(item -> !(item instanceof RoutingObservabilityAdapter))
                .toList();
        return new RoutingObservabilityAdapter(adapterProperties, externalAdapterRegistry, builtIn);
    }

    @Bean
    @Primary
    public AgentRuntimeAdapter routingAgentRuntimeAdapter(LlmAdapterProperties adapterProperties,
                                                            ExternalAdapterRegistry externalAdapterRegistry,
                                                            ObjectProvider<AgentRuntimeAdapter> adapterProvider) {
        List<AgentRuntimeAdapter> builtIn = adapterProvider.stream()
                .filter(item -> !(item instanceof RoutingAgentRuntimeAdapter))
                .toList();
        return new RoutingAgentRuntimeAdapter(adapterProperties, externalAdapterRegistry, builtIn);
    }

    @Bean
    @Primary
    public KnowledgeRetrievalAdapter routingKnowledgeRetrievalAdapter(LlmAdapterProperties adapterProperties,
                                                                        ExternalAdapterRegistry externalAdapterRegistry,
                                                                        ObjectProvider<KnowledgeRetrievalAdapter> adapterProvider) {
        List<KnowledgeRetrievalAdapter> builtIn = adapterProvider.stream()
                .filter(item -> !(item instanceof RoutingKnowledgeRetrievalAdapter))
                .toList();
        return new RoutingKnowledgeRetrievalAdapter(adapterProperties, externalAdapterRegistry, builtIn);
    }

    @Bean
    @Primary
    public LearningPipelineAdapter routingLearningPipelineAdapter(LlmAdapterProperties adapterProperties,
                                                                    ExternalAdapterRegistry externalAdapterRegistry,
                                                                    ObjectProvider<LearningPipelineAdapter> adapterProvider) {
        List<LearningPipelineAdapter> builtIn = adapterProvider.stream()
                .filter(item -> !(item instanceof RoutingLearningPipelineAdapter))
                .toList();
        return new RoutingLearningPipelineAdapter(adapterProperties, externalAdapterRegistry, builtIn);
    }
}
