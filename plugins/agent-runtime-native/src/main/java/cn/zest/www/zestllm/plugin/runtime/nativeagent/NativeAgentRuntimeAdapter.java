package cn.zest.www.zestllm.plugin.runtime.nativeagent;

import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeInvokeRequest;
import lombok.RequiredArgsConstructor;

/**
 * native 妯″紡锛氫笟鍔′晶鐩磋繛 LiteLLM锛涙湰閫傞厤鍣ㄤ富瑕佺敤浜?health 涓?Probe 鍗犱綅銆?
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
                "runtimeMode=native 鏃?execute 鐢变笟鍔?Agent 鐩磋繛 LiteLLM锛屼笉缁?AgentRuntimeAdapter.invoke");
    }

    @Override
    public HealthStatus health() {
        return modelGatewayAdapter.health();
    }
}


