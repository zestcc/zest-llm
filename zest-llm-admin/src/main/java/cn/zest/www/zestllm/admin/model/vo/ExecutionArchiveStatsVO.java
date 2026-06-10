package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionArchiveStatsVO {
    private long hotExecutions;
    private long archivedExecutions;
    private int retentionDays;
    private boolean archiveEnabled;
}
