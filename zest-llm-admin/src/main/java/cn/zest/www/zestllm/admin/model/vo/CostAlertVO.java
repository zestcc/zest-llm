package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CostAlertVO {
    private String appKey;
    private LocalDate alertDate;
    private BigDecimal dailyCost;
    private BigDecimal costLimit;
    private Integer thresholdPct;
    private String status;
    private LocalDateTime createdAt;
}
