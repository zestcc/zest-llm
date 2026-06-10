package cn.zest.www.zestllm.infra.tool;

import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.spi.tool.McpToolCallRequest;
import cn.zest.www.zestllm.spi.tool.McpToolCallResult;
import cn.zest.www.zestllm.spi.tool.McpToolDescriptor;
import cn.zest.www.zestllm.spi.tool.McpToolListRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class HttpMcpToolAdapter implements McpToolAdapter {

    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public McpToolCallResult call(McpToolCallRequest request) {
        if (request.getServerUrl() == null || request.getToolName() == null) {
            return McpToolCallResult.builder()
                    .success(false)
                    .toolName(request.getToolName())
                    .errorMessage("MCP serverUrl or toolName missing")
                    .build();
        }
        try {
            ObjectNode rpc = objectMapper.createObjectNode();
            rpc.put("jsonrpc", "2.0");
            rpc.put("id", 1);
            rpc.put("method", "tools/call");
            ObjectNode params = rpc.putObject("params");
            params.put("name", request.getToolName());
            params.set("arguments", objectMapper.valueToTree(
                    request.getArguments() != null ? request.getArguments() : Map.of()));

            RestClient.Builder builder = restClientBuilder.baseUrl(request.getServerUrl());
            if (request.getServerAuthToken() != null && !request.getServerAuthToken().isBlank()) {
                builder.defaultHeader("Authorization", "Bearer " + request.getServerAuthToken());
            }
            String raw = builder.build()
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rpc.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            if (root.has("error")) {
                return McpToolCallResult.builder()
                        .success(false)
                        .toolName(request.getToolName())
                        .errorMessage(root.path("error").path("message").asText("MCP error"))
                        .build();
            }
            JsonNode resultNode = root.path("result");
            Map<String, Object> content = new HashMap<>();
            if (resultNode.isObject()) {
                content = objectMapper.convertValue(resultNode, Map.class);
            } else if (!resultNode.isMissingNode()) {
                content.put("value", resultNode.asText());
            }
            return McpToolCallResult.builder()
                    .success(true)
                    .toolName(request.getToolName())
                    .content(content)
                    .build();
        } catch (Exception ex) {
            log.warn("MCP tool call failed tool={} traceId={}", request.getToolName(), request.getTraceId(), ex);
            return McpToolCallResult.builder()
                    .success(false)
                    .toolName(request.getToolName())
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    @Override
    public List<McpToolDescriptor> listTools(McpToolListRequest request) {
        if (request.getServerUrl() == null || request.getServerUrl().isBlank()) {
            return List.of();
        }
        try {
            ObjectNode rpc = objectMapper.createObjectNode();
            rpc.put("jsonrpc", "2.0");
            rpc.put("id", 1);
            rpc.put("method", "tools/list");
            rpc.set("params", objectMapper.createObjectNode());

            RestClient.Builder builder = restClientBuilder.baseUrl(request.getServerUrl());
            if (request.getServerAuthToken() != null && !request.getServerAuthToken().isBlank()) {
                builder.defaultHeader("Authorization", "Bearer " + request.getServerAuthToken());
            }
            String raw = builder.build()
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rpc.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            JsonNode tools = root.path("result").path("tools");
            if (!tools.isArray()) {
                return List.of();
            }
            List<McpToolDescriptor> descriptors = new ArrayList<>();
            for (JsonNode tool : tools) {
                String name = tool.hasNonNull("name") ? tool.get("name").asText() : null;
                String description = tool.hasNonNull("description") ? tool.get("description").asText() : null;
                descriptors.add(McpToolDescriptor.builder()
                        .name(name)
                        .description(description)
                        .inputSchema(tool.has("inputSchema")
                                ? objectMapper.convertValue(tool.get("inputSchema"), Object.class)
                                : null)
                        .build());
            }
            return descriptors;
        } catch (Exception ex) {
            log.warn("MCP tools/list failed serverUrl={}", request.getServerUrl(), ex);
            return List.of();
        }
    }
}
