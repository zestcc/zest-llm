package cn.zest.www.zestllm.infra.tool;

import cn.zest.www.zestllm.spi.tool.McpToolCallRequest;
import cn.zest.www.zestllm.spi.tool.McpToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpMcpToolAdapterTest {

    private static WireMockServer wireMock;
    private static HttpMcpToolAdapter adapter;

    @BeforeAll
    static void start() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        wireMock.stubFor(post(urlEqualTo("/mcp"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"ok"}]}}
                                """)));
        adapter = new HttpMcpToolAdapter(new ObjectMapper(), RestClient.builder());
    }

    @AfterAll
    static void stop() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void call_returnsSuccess() {
        McpToolCallResult result = adapter.call(McpToolCallRequest.builder()
                .serverUrl("http://localhost:" + wireMock.port() + "/mcp")
                .toolName("search")
                .traceId("tr_1")
                .build());
        assertTrue(result.isSuccess());
    }
}
