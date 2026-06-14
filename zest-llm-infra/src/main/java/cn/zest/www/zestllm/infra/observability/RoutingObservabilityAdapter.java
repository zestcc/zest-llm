package cn.zest.www.zestllm.infra.observability;

import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.infra.plugin.RoutingAdapterSupport;
import cn.zest.www.zestllm.spi.model.TraceEndEvent;
import cn.zest.www.zestllm.spi.model.TraceStartEvent;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;

import java.util.List;

public class RoutingObservabilityAdapter implements ObservabilityAdapter {

    private final LlmAdapterProperties adapterProperties;
    private final ExternalAdapterRegistry externalRegistry;
    private final List<ObservabilityAdapter> builtInAdapters;

    public RoutingObservabilityAdapter(LlmAdapterProperties adapterProperties,
                                       ExternalAdapterRegistry externalRegistry,
                                       List<ObservabilityAdapter> builtInAdapters) {
        this.adapterProperties = adapterProperties;
        this.externalRegistry = externalRegistry;
        this.builtInAdapters = builtInAdapters;
    }

    @Override
    public String adapterId() {
        return delegate().adapterId();
    }

    @Override
    public void traceStart(TraceStartEvent event) {
        delegate().traceStart(event);
    }

    @Override
    public void traceEnd(TraceEndEvent event) {
        delegate().traceEnd(event);
    }

    private ObservabilityAdapter delegate() {
        String configured = adapterProperties.getObservability();
        return RoutingAdapterSupport.resolve(
                configured,
                externalRegistry.observability(configured),
                builtInAdapters,
                RoutingObservabilityAdapter.class,
                ObservabilityAdapter::adapterId,
                "ObservabilityAdapter");
    }
}
