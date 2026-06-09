package cn.zest.www.zestllm.common.api;

import lombok.Data;

import java.util.List;

@Data
public class PrepareResponse {
    private String traceId;
    private String code;
    private String promptVersion;
    private String renderedPrompt;
    private String model;
    private List<String> fallbackModels;
    private Integer maxTokens;
    private Double temperature;
    private Integer timeoutMs;
    private String outputSchema;
}
