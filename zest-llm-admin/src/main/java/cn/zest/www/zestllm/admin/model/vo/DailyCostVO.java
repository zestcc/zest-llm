package cn.zest.www.zestllm.admin.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyCostVO {
    private LocalDate date;
    private BigDecimal totalCost;
    private Long callCount;
    private Long promptTokens;
    private Long completionTokens;
}
