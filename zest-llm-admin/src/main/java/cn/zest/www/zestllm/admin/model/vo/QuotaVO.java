package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class QuotaVO {
    private String appKey;
    private Long dailyTokenLimit;
    private Integer qpsLimit;
    private BigDecimal dailyCostLimit;
    private LocalDateTime updatedAt;
}
