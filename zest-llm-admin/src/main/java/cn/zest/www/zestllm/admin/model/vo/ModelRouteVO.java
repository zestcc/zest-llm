package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ModelRouteVO {
    private Long id;
    private String taskCode;
    private String primaryModel;
    private String fallbackModels;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Integer timeoutMs;
    private String policyJson;
    private String status;
    private LocalDateTime updatedAt;
}
