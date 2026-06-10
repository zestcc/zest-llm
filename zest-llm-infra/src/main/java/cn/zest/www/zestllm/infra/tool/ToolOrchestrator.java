package cn.zest.www.zestllm.infra.tool;

import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.spi.tool.McpToolCallRequest;
import cn.zest.www.zestllm.spi.tool.McpToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ToolOrchestrator {

    private final McpToolAdapter mcpToolAdapter;
    private final SecretResolver secretResolver;
    private final ObjectMapper objectMapper;

    public String enrichPrompt(String renderedPrompt,
                               List<ToolDefinition> tools,
                               Map<String, Object> inputs,
                               String traceId) {
        if (tools == null || tools.isEmpty()) {
            return renderedPrompt;
        }
        List<String> sections = new ArrayList<>();
        for (ToolDefinition tool : tools) {
            if (tool == null || !"mcp".equalsIgnoreCase(tool.getType())) {
                continue;
            }
            McpToolCallResult result = invokeMcpTool(tool, inputs, traceId);
            if (result.isSuccess()) {
                try {
                    sections.add("[MCP " + result.getToolName() + "]\n"
                            + objectMapper.writeValueAsString(result.getContent()));
                } catch (Exception ex) {
                    sections.add("[MCP " + result.getToolName() + "]\n" + result.getContent());
                }
            } else {
                log.warn("MCP tool skipped tool={} traceId={} reason={}",
                        tool.getName(), traceId, result.getErrorMessage());
            }
        }
        if (sections.isEmpty()) {
            return renderedPrompt;
        }
        return String.join("\n\n", sections) + "\n\n" + renderedPrompt;
    }

    public String resolveGatewayApiKey(String outboundSecretRef, String fallbackKey) {
        if (outboundSecretRef != null) {
            return secretResolver.resolve(outboundSecretRef).orElse(fallbackKey);
        }
        return fallbackKey;
    }

    public ToolDefinition findTool(List<ToolDefinition> tools, String name) {
        if (tools == null || name == null) {
            return null;
        }
        for (ToolDefinition tool : tools) {
            if (tool == null) {
                continue;
            }
            String toolName = asString(tool.getConfig().get("toolName"));
            if (toolName == null || toolName.isBlank()) {
                toolName = tool.getName();
            }
            if (name.equals(toolName) || name.equals(tool.getName())) {
                return tool;
            }
        }
        return null;
    }

    public McpToolCallResult callTool(ToolDefinition tool, String toolName, Map<String, Object> arguments, String traceId) {
        return invokeMcpTool(tool, toolName, arguments, traceId);
    }

    public ArrayNode buildOpenAiTools(List<ToolDefinition> tools) {
        ArrayNode array = objectMapper.createArrayNode();
        if (tools == null) {
            return array;
        }
        for (ToolDefinition tool : tools) {
            if (tool == null || !"mcp".equalsIgnoreCase(tool.getType())) {
                continue;
            }
            String fnName = asString(tool.getConfig().get("toolName"));
            if (fnName == null || fnName.isBlank()) {
                fnName = tool.getName();
            }
            ObjectNode fn = objectMapper.createObjectNode();
            fn.put("name", fnName);
            fn.put("description", asString(tool.getConfig().get("description")) != null
                    ? asString(tool.getConfig().get("description"))
                    : "MCP tool " + fnName);
            Object parameters = tool.getConfig().get("parameters");
            if (parameters != null) {
                fn.set("parameters", objectMapper.valueToTree(parameters));
            } else {
                fn.set("parameters", objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode()));
            }
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("type", "function");
            entry.set("function", fn);
            array.add(entry);
        }
        return array;
    }

    public static boolean shouldUseToolLoop(List<ToolDefinition> tools, String toolCallMode) {
        if (tools == null || tools.isEmpty()) {
            return false;
        }
        return !"prefetch".equalsIgnoreCase(toolCallMode);
    }

    private McpToolCallResult invokeMcpTool(ToolDefinition tool, Map<String, Object> inputs, String traceId) {
        String toolName = asString(tool.getConfig().get("toolName"));
        if (toolName == null || toolName.isBlank()) {
            toolName = tool.getName();
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        Object argConfig = tool.getConfig().get("arguments");
        if (argConfig instanceof Map<?, ?> map) {
            map.forEach((k, v) -> arguments.put(String.valueOf(k), v));
        }
        if (inputs != null) {
            arguments.putAll(inputs);
        }
        return invokeMcpTool(tool, toolName, arguments, traceId);
    }

    private McpToolCallResult invokeMcpTool(ToolDefinition tool, String toolName,
                                            Map<String, Object> arguments, String traceId) {
        String serverUrl = asString(tool.getConfig().get("serverUrl"));
        String authRef = asString(tool.getConfig().get("authSecretRef"));
        String authToken = authRef != null ? secretResolver.resolve(authRef).orElse(null) : null;

        return mcpToolAdapter.call(McpToolCallRequest.builder()
                .serverUrl(serverUrl)
                .serverAuthToken(authToken)
                .toolName(toolName)
                .arguments(arguments)
                .traceId(traceId)
                .build());
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
