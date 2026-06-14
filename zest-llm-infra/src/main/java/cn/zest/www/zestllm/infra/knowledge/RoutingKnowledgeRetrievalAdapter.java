package cn.zest.www.zestllm.infra.knowledge;

import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.infra.plugin.RoutingAdapterSupport;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.model.HealthStatus;

import java.util.List;

/**
 * 路由 Knowledge SPI：优先外置 JAR，其次 Spring 内置 Bean。
 */
public class RoutingKnowledgeRetrievalAdapter implements KnowledgeRetrievalAdapter {

    private final LlmAdapterProperties adapterProperties;
    private final ExternalAdapterRegistry externalRegistry;
    private final List<KnowledgeRetrievalAdapter> builtInAdapters;

    public RoutingKnowledgeRetrievalAdapter(LlmAdapterProperties adapterProperties,
                                            ExternalAdapterRegistry externalRegistry,
                                            List<KnowledgeRetrievalAdapter> adapters) {
        this.adapterProperties = adapterProperties;
        this.externalRegistry = externalRegistry;
        this.builtInAdapters = adapters.stream()
                .filter(item -> !(item instanceof RoutingKnowledgeRetrievalAdapter))
                .toList();
    }

    @Override
    public String adapterId() {
        return delegate().adapterId();
    }

    @Override
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        return delegate().retrieve(request);
    }

    @Override
    public HealthStatus health() {
        return delegate().health();
    }

    private KnowledgeRetrievalAdapter delegate() {
        String configured = adapterProperties.getKnowledgeRetrieval();
        return RoutingAdapterSupport.resolve(
                configured,
                externalRegistry.knowledge(configured),
                builtInAdapters,
                RoutingKnowledgeRetrievalAdapter.class,
                KnowledgeRetrievalAdapter::adapterId,
                "KnowledgeRetrievalAdapter");
    }
}
