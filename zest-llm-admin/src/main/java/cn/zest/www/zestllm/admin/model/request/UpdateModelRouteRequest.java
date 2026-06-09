package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateModelRouteRequest {
    @NotBlank
    private String primaryModel;
    private String fallbackModels;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Integer timeoutMs;
    private String policyJson;
}
