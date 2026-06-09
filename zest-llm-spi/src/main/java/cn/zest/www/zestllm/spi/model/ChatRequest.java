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
    private Map<String, String> headers;
}
