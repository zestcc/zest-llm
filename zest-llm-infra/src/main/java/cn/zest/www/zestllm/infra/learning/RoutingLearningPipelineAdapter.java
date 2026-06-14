package cn.zest.www.zestllm.infra.learning;

import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.infra.plugin.RoutingAdapterSupport;
import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.LearningCycleRequest;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import cn.zest.www.zestllm.spi.model.HealthStatus;

import java.util.List;

public class RoutingLearningPipelineAdapter implements LearningPipelineAdapter {

    private final LlmAdapterProperties adapterProperties;
    private final ExternalAdapterRegistry externalRegistry;
    private final List<LearningPipelineAdapter> builtInAdapters;

    public RoutingLearningPipelineAdapter(LlmAdapterProperties adapterProperties,
                                          ExternalAdapterRegistry externalRegistry,
                                          List<LearningPipelineAdapter> builtInAdapters) {
        this.adapterProperties = adapterProperties;
        this.externalRegistry = externalRegistry;
        this.builtInAdapters = builtInAdapters;
    }

    @Override
    public String adapterId() {
        return delegate().adapterId();
    }

    @Override
    public LearningCycleResult runCycle(LearningCycleRequest request) {
        return delegate().runCycle(request);
    }

    @Override
    public List<EvalCaseSuggestion> suggestCasesFromTraces(TraceSampleQuery query) {
        return delegate().suggestCasesFromTraces(query);
    }

    @Override
    public HealthStatus health() {
        return delegate().health();
    }

    private LearningPipelineAdapter delegate() {
        String configured = adapterProperties.getLearningPipeline();
        return RoutingAdapterSupport.resolve(
                configured,
                externalRegistry.learningPipeline(configured),
                builtInAdapters,
                RoutingLearningPipelineAdapter.class,
                LearningPipelineAdapter::adapterId,
                "LearningPipelineAdapter");
    }
}
