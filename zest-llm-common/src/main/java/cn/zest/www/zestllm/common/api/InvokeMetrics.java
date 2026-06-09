package cn.zest.www.zestllm.common.api;

import lombok.Data;

@Data
public class InvokeMetrics {
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Double cost;
}
