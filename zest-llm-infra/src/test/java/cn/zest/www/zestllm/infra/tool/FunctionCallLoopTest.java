package cn.zest.www.zestllm.infra.tool;

import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.spi.tool.McpToolCallResult;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunctionCallLoopTest {

    static WireMockServer wireMock;

    @Mock
    private McpToolAdapter mcpToolAdapter;
    @Mock
    private SecretResolver secretResolver;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void run_executesToolThenReturnsFinalAnswer() {
        wireMock.resetAll();
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("tool-loop")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"choices":[{"message":{"role":"assistant","tool_calls":[{"id":"call_1","type":"function","function":{"name":"search","arguments":"{\\"question\\":\\"hi\\"}"}}]}}],"usage":{"prompt_tokens":1,"completion_tokens":1}}
                                """))
                .willSetStateTo("after-tool"));
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("tool-loop")
                .whenScenarioStateIs("after-tool")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"choices":[{"message":{"role":"assistant","content":"{\\"answer\\":\\"done\\"}"}}],"usage":{"prompt_tokens":2,"completion_tokens":3}}
                                """)));

        when(mcpToolAdapter.call(any())).thenReturn(McpToolCallResult.builder()
                .success(true)
                .toolName("search")
                .content(Map.of("snippet", "doc"))
                .build());

        ObjectMapper objectMapper = new ObjectMapper();
        ToolOrchestrator orchestrator = new ToolOrchestrator(mcpToolAdapter, secretResolver, objectMapper);
        FunctionCallLoop loop = new FunctionCallLoop(orchestrator, objectMapper);

        ToolDefinition tool = new ToolDefinition();
        tool.setType("mcp");
        tool.setName("search");
        tool.getConfig().put("serverUrl", "http://localhost/mcp");
        tool.getConfig().put("toolName", "search");

        ToolLoopParams params = ToolLoopParams.builder()
                .traceId("tr_loop")
                .tools(List.of(tool))
                .maxToolSteps(3)
                .build();

        RestClient client = RestClient.builder().baseUrl("http://localhost:" + wireMock.port()).build();
        FunctionCallLoop.LoopResult result = loop.run(client, params, "gpt-4o-mini", "Question", Map.of("question", "hi"));

        assertTrue(result.content().contains("done"));
        wireMock.verify(2, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }
}
