package cn.zest.www.zestllm.plugin.runtime.dify;

import cn.zest.www.zestllm.plugin.dify.common.DifyProperties;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AgentRuntimeDifyAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.agent-runtime", havingValue = "dify")
    public AgentRuntimeAdapter difyAgentRuntimeAdapter(DifyProperties properties,
                                                       SecretResolver secretResolver,
                                                       ObjectMapper objectMapper) {
        return new DifyAgentRuntimeAdapter(properties, secretResolver, objectMapper);
    }
}
