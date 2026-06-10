package cn.zest.www.zestllm.spi.alert;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CostAlertEvent {
    private String appKey;
    private LocalDate alertDate;
    private BigDecimal dailyCost;
    private BigDecimal costLimit;
    private int thresholdPct;
    private String message;
}
