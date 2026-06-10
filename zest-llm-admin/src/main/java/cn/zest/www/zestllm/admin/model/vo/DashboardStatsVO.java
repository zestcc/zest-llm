package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardStatsVO {
    private long apps;
    private long executions;
    private long success;
    private long failed;
    private BigDecimal totalCost;
    private long todayExecutions;
    private long agentsMonitored;
    private long agentsReady;
    private long agentsDegraded;
    private long agentsUnavailable;
    private long agentsUnknown;
}
