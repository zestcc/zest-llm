package cn.zest.www.zestllm.spi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TraceEndEvent {
    private String traceId;
    private boolean success;
    private String errorCode;
    private Integer promptTokens;
    private Integer completionTokens;
    private Double cost;
    private Long latencyMs;
}
