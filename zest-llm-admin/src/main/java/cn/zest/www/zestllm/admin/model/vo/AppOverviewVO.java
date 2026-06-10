package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppOverviewVO {
    private String appKey;
    private String appName;
    private String status;
    private long taskCount;
    private long methodCount;
    private long executionsToday;
    private long failedToday;
}
