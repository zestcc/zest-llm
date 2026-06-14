package cn.zest.www.zestllm.infra.gateway;

import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.infra.plugin.RoutingAdapterSupport;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.model.ChatRequest;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;

import java.util.List;

public class RoutingModelGatewayAdapter implements ModelGatewayAdapter {

    private final LlmAdapterProperties adapterProperties;
    private final ExternalAdapterRegistry externalRegistry;
    private final List<ModelGatewayAdapter> builtInAdapters;

    public RoutingModelGatewayAdapter(LlmAdapterProperties adapterProperties,
                                      ExternalAdapterRegistry externalRegistry,
                                      List<ModelGatewayAdapter> builtInAdapters) {
        this.adapterProperties = adapterProperties;
        this.externalRegistry = externalRegistry;
        this.builtInAdapters = builtInAdapters;
    }

    @Override
    public String adapterId() {
        return delegate().adapterId();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return delegate().chat(request);
    }

    @Override
    public HealthStatus health() {
        return delegate().health();
    }

    private ModelGatewayAdapter delegate() {
        String configured = adapterProperties.getModelGateway();
        return RoutingAdapterSupport.resolve(
                configured,
                externalRegistry.modelGateway(configured),
                builtInAdapters,
                RoutingModelGatewayAdapter.class,
                ModelGatewayAdapter::adapterId,
                "ModelGatewayAdapter");
    }
}
