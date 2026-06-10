package cn.zest.www.zestllm.spi.report;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ReportDelivery {
    private String traceId;
    private String appKey;
    private String code;
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
