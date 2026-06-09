package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionVO {
    private String traceId;
    private String taskCode;
    private String bizId;
    private String promptVersion;
    private String model;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorCode;
    private String errorMessage;
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private BigDecimal cost;
    private LocalDateTime createdAt;
}
