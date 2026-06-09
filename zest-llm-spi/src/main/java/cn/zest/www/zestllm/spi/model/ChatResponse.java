package cn.zest.www.zestllm.spi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String model;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Double cost;
    private Long latencyMs;
}
