package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateQuotaRequest {
    private Long dailyTokenLimit;
    private Integer qpsLimit;
    private BigDecimal dailyCostLimit;
    private String alertWebhookUrl;
    private Integer alertThresholdPct;
}
