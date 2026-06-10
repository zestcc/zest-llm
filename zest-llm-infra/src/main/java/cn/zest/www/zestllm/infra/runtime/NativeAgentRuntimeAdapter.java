package cn.zest.www.zestllm.infra.runtime;

import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeInvokeRequest;
import lombok.RequiredArgsConstructor;

/**
 * native 模式：业务侧直连 LiteLLM；本适配器主要用于 health 与 Probe 占位。
 */
@RequiredArgsConstructor
public class NativeAgentRuntimeAdapter implements AgentRuntimeAdapter {

    private final ModelGatewayAdapter modelGatewayAdapter;

    @Override
    public String adapterId() {
        return "native";
    }

    @Override
    public ChatResponse invoke(AgentRuntimeInvokeRequest request) {
        throw new UnsupportedOperationException(
                "runtimeMode=native 时 execute 由业务 Agent 直连 LiteLLM，不经 AgentRuntimeAdapter.invoke");
    }

    @Override
    public HealthStatus health() {
        return modelGatewayAdapter.health();
    }
}
