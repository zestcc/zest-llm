package cn.zest.www.zestllm.common.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class ReportRequest {
    @NotBlank
    private String traceId;
    @NotBlank
    private String appKey;
    @NotBlank
    private String code;
    private String bizId;
    @NotBlank
    private String status;
    private String model;
    private String promptVersion;
    private Map<String, Object> output;
    private String errorCode;
    private String errorMessage;
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Double cost;
}
