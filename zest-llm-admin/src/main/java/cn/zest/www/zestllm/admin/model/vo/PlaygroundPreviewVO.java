package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;

import java.util.List;

@Data
@Builder
public class PlaygroundPreviewVO {
    private String traceId;
    private String code;
    private String promptVersion;
    private String renderedPrompt;
    private String model;
    private List<String> fallbackModels;
    private Integer maxTokens;
    private Double temperature;
    private String outputSchema;
    private GuardrailsConfig guardrails;
}
