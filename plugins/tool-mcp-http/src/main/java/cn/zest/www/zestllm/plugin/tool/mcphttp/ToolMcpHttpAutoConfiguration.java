package cn.zest.www.zestllm.plugin.tool.mcphttp;

import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class ToolMcpHttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(McpToolAdapter.class)
    public McpToolAdapter httpMcpToolAdapter(ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        return new HttpMcpToolAdapter(objectMapper, restClientBuilder);
    }
}
