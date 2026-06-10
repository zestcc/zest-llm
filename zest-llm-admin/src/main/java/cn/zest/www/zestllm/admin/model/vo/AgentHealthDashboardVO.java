package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentHealthDashboardVO {
    private long monitored;
    private long ready;
    private long degraded;
    private long unavailable;
    private long unknown;
    private List<AgentHealthItemVO> alerts;
}
