package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentProfileProbeTrendPointVO {
    private String date;
    private long ready;
    private long degraded;
    private long unavailable;
    private long total;
}
