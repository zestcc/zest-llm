package cn.zest.www.zestllm.plugin.gateway.litellm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * LiteLLM 网关对外协议：OpenAI Compatible 或 Anthropic Messages API。
 */
public final class GatewayApiProtocol {

    public static final String OPENAI = "openai";
    public static final String ANTHROPIC = "anthropic";

    private GatewayApiProtocol() {
    }

    public static String normalize(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            return OPENAI;
        }
        String value = protocol.trim().toLowerCase();
        if ("anthropic".equals(value) || "anthropic-messages".equals(value)) {
            return ANTHROPIC;
        }
        return OPENAI;
    }

    public static boolean isAnthropic(String protocol) {
        return ANTHROPIC.equals(normalize(protocol));
    }

    /** 模型级 > Provider 级 > 全局默认 */
    public static String resolveEffective(String modelProtocol, String gatewayProtocol, String defaultProtocol) {
        if (modelProtocol != null && !modelProtocol.isBlank()) {
            return normalize(modelProtocol);
        }
        if (gatewayProtocol != null && !gatewayProtocol.isBlank()) {
            return normalize(gatewayProtocol);
        }
        return normalize(defaultProtocol);
    }

    public static String extractAnthropicText(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (!contentNode.isArray()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : contentNode) {
            if ("text".equals(block.path("type").asText()) && block.has("text")) {
                text.append(block.path("text").asText(""));
            }
        }
        return text.toString();
    }
}

