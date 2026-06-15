package cn.zest.www.zestllm.infra.config;

import cn.zest.www.zestllm.infra.tool.FunctionCallLoop;
import cn.zest.www.zestllm.infra.tool.ToolOrchestrator;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 工具编排须在 MCP 插件 AutoConfiguration 之后注册，避免 {@link ToolOrchestrator} 早于 {@link McpToolAdapter} 创建。
 */
@AutoConfiguration(afterName = {
        "cn.zest.www.zestllm.plugin.tool.mcphttp.ToolMcpHttpAutoConfiguration"
})
public class LlmToolAutoConfiguration {

    @Bean
    @ConditionalOnBean(McpToolAdapter.class)
    @ConditionalOnMissingBean
    public ToolOrchestrator toolOrchestrator(McpToolAdapter mcpToolAdapter,
                                             SecretResolver secretResolver,
                                             ObjectMapper objectMapper) {
        return new ToolOrchestrator(mcpToolAdapter, secretResolver, objectMapper);
    }

    @Bean
    @ConditionalOnBean(ToolOrchestrator.class)
    @ConditionalOnMissingBean
    public FunctionCallLoop functionCallLoop(ToolOrchestrator toolOrchestrator, ObjectMapper objectMapper) {
        return new FunctionCallLoop(toolOrchestrator, objectMapper);
    }
}
