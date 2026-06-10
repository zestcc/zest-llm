package cn.zest.www.zestllm.infra.runtime;

import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeInvokeRequest;

public class NoopAgentRuntimeAdapter implements AgentRuntimeAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public ChatResponse invoke(AgentRuntimeInvokeRequest request) {
        return ChatResponse.builder()
                .content("")
                .latencyMs(0L)
                .build();
    }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("noop agent runtime").build();
    }
}
