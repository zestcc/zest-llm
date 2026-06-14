package cn.zest.www.zestllm.plugin.runtime.nativeagent;

import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AgentRuntimeNativeAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.agent-runtime", havingValue = "native", matchIfMissing = true)
    public AgentRuntimeAdapter nativeAgentRuntimeAdapter(ModelGatewayAdapter modelGatewayAdapter) {
        return new NativeAgentRuntimeAdapter(modelGatewayAdapter);
    }
}
