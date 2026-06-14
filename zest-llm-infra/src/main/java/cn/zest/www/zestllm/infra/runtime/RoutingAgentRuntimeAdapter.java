package cn.zest.www.zestllm.infra.runtime;

import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.infra.plugin.RoutingAdapterSupport;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeInvokeRequest;

import java.util.List;

public class RoutingAgentRuntimeAdapter implements AgentRuntimeAdapter {

    private final LlmAdapterProperties adapterProperties;
    private final ExternalAdapterRegistry externalRegistry;
    private final List<AgentRuntimeAdapter> builtInAdapters;

    public RoutingAgentRuntimeAdapter(LlmAdapterProperties adapterProperties,
                                      ExternalAdapterRegistry externalRegistry,
                                      List<AgentRuntimeAdapter> builtInAdapters) {
        this.adapterProperties = adapterProperties;
        this.externalRegistry = externalRegistry;
        this.builtInAdapters = builtInAdapters;
    }

    @Override
    public String adapterId() {
        return delegate().adapterId();
    }

    @Override
    public ChatResponse invoke(AgentRuntimeInvokeRequest request) {
        return delegate().invoke(request);
    }

    @Override
    public HealthStatus health() {
        return delegate().health();
    }

    private AgentRuntimeAdapter delegate() {
        String configured = adapterProperties.getAgentRuntime();
        return RoutingAdapterSupport.resolve(
                configured,
                externalRegistry.agentRuntime(configured),
                builtInAdapters,
                RoutingAgentRuntimeAdapter.class,
                AgentRuntimeAdapter::adapterId,
                "AgentRuntimeAdapter");
    }
}
