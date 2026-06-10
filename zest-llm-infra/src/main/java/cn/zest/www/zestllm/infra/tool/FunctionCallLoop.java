package cn.zest.www.zestllm.infra.tool;

import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import cn.zest.www.zestllm.spi.tool.McpToolCallResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI/LiteLLM function calling 多轮循环（CP 与 Agent 共用）。
 */
@Slf4j
@RequiredArgsConstructor
public class FunctionCallLoop {

    private final ToolOrchestrator toolOrchestrator;
    private final ObjectMapper objectMapper;

    public LoopResult run(RestClient client,
                          ToolLoopParams params,
                          String model,
                          String userPrompt,
                          Map<String, Object> inputs) {
        int maxSteps = params.maxToolSteps() != null && params.maxToolSteps() > 0
                ? params.maxToolSteps() : 5;
        List<ToolDefinition> tools = params.tools();
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", userPrompt));

        JsonNode lastUsage = null;
        for (int step = 0; step < maxSteps; step++) {
            ObjectNode body = buildRequestBody(params, model, messages, tools);
            String raw = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode root;
            try {
                root = objectMapper.readTree(raw);
            } catch (Exception ex) {
                throw new RestClientException("Invalid LiteLLM response", ex);
            }
            lastUsage = root.path("usage");
            JsonNode choice = root.path("choices").path(0);
            JsonNode message = choice.path("message");
            JsonNode toolCalls = message.path("tool_calls");

            if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                messages.add(message.deepCopy());
                for (JsonNode toolCall : toolCalls) {
                    String toolCallId = toolCall.path("id").asText("call_" + step);
                    String fnName = toolCall.path("function").path("name").asText("");
                    String argsJson = toolCall.path("function").path("arguments").asText("{}");
                    Map<String, Object> args = parseArguments(argsJson, inputs);
                    ToolDefinition def = toolOrchestrator.findTool(tools, fnName);
                    String toolContent;
                    if (def != null) {
                        McpToolCallResult result = toolOrchestrator.callTool(
                                def, fnName, args, params.traceId());
                        toolContent = result.isSuccess()
                                ? stringify(result.getContent())
                                : "error: " + result.getErrorMessage();
                    } else {
                        toolContent = "error: unknown tool " + fnName;
                    }
                    ObjectNode toolMessage = objectMapper.createObjectNode();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCallId);
                    toolMessage.put("content", toolContent);
                    messages.add(toolMessage);
                }
                continue;
            }

            String content = message.path("content").asText("");
            return new LoopResult(content, lastUsage, step + 1, messages.deepCopy());
        }
        throw new RestClientException("Max tool steps exceeded: " + maxSteps);
    }

    private ObjectNode buildRequestBody(ToolLoopParams params,
                                        String model,
                                        ArrayNode messages,
                                        List<ToolDefinition> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        if (params.maxTokens() != null) {
            body.put("max_tokens", params.maxTokens());
        }
        if (params.temperature() != null) {
            body.put("temperature", params.temperature());
        }
        body.set("messages", messages);
        ArrayNode openAiTools = toolOrchestrator.buildOpenAiTools(tools);
        if (!openAiTools.isEmpty()) {
            body.set("tools", openAiTools);
            body.put("tool_choice", "auto");
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argsJson, Map<String, Object> inputs) {
        Map<String, Object> args = new HashMap<>();
        try {
            JsonNode node = objectMapper.readTree(argsJson);
            if (node.isObject()) {
                node.fields().forEachRemaining(e -> args.put(e.getKey(), objectMapper.convertValue(e.getValue(), Object.class)));
            }
        } catch (Exception ex) {
            log.debug("Failed to parse tool arguments: {}", argsJson);
        }
        if (inputs != null) {
            args.putAll(inputs);
        }
        return args;
    }

    private String stringify(Object content) {
        if (content == null) {
            return "";
        }
        if (content instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception ex) {
            return String.valueOf(content);
        }
    }

    public record LoopResult(String content, JsonNode usage, int stepsUsed, JsonNode messages) {
    }
}
