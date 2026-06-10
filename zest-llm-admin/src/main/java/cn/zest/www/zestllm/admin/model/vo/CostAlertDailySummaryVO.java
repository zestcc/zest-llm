package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CostAlertDailySummaryVO {
    private String date;
    private String appKey;
    private long alertCount;
    private long sentCount;
}
