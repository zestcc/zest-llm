package cn.zest.www.zestllm.spi.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatRequest {
    private String traceId;
    private String model;
    private String systemPrompt;
    private String userMessage;
    private Integer maxTokens;
    private Double temperature;
    private List<String> fallbackModels;
    /** openai | anthropic，未设时使用 LiteLLMProperties.defaultApiProtocol */
    private String apiProtocol;
    /** 覆盖全局 LiteLLM baseUrl（来自 Provider 预设） */
    private String baseUrl;
    /** 覆盖全局 LiteLLM apiKey（来自 outboundAuth） */
    private String apiKey;
    private Map<String, String> headers;
}
