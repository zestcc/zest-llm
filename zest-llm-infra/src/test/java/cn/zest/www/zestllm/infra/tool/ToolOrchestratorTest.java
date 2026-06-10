package cn.zest.www.zestllm.infra.tool;

import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.spi.tool.McpToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolOrchestratorTest {

    @Mock
    private McpToolAdapter mcpToolAdapter;
    @Mock
    private SecretResolver secretResolver;

    @Test
    void enrichPrompt_appendsMcpResult() {
        ToolDefinition tool = new ToolDefinition();
        tool.setType("mcp");
        tool.setName("search");
        tool.setServerRef("internal-docs");
        tool.getConfig().put("serverUrl", "http://localhost:9090/mcp");
        tool.getConfig().put("toolName", "search");

        when(mcpToolAdapter.call(ArgumentMatchers.any())).thenReturn(McpToolCallResult.builder()
                .success(true)
                .toolName("search")
                .content(Map.of("snippet", "doc-content"))
                .build());

        ToolOrchestrator orchestrator = new ToolOrchestrator(
                mcpToolAdapter, secretResolver, new ObjectMapper());

        String enriched = orchestrator.enrichPrompt("User question", List.of(tool), Map.of("question", "hello"), "tr_1");
        assertTrue(enriched.contains("doc-content"));
        assertTrue(enriched.contains("User question"));
    }

    @Test
    void resolveGatewayApiKey_usesSecretRef() {
        when(secretResolver.resolve("env:LITELLM_API_KEY")).thenReturn(Optional.of("sk-test"));
        ToolOrchestrator orchestrator = new ToolOrchestrator(
                mcpToolAdapter, secretResolver, new ObjectMapper());
        assertTrue(orchestrator.resolveGatewayApiKey("env:LITELLM_API_KEY", "fallback").contains("sk-test"));
    }
}
