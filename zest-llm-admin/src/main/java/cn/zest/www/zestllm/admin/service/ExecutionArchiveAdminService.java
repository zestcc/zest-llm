package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.ExecutionArchiveProperties;
import cn.zest.www.zestllm.admin.mapper.LlmExecutionMapper;
import cn.zest.www.zestllm.admin.model.vo.ExecutionArchiveStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutionArchiveAdminService {

    private final ExecutionArchiveProperties properties;
    private final LlmExecutionMapper executionMapper;
    private final ExecutionArchiveService executionArchiveService;

    private volatile LocalDateTime lastRunAt;
    private volatile int lastArchivedCount;
    private volatile int lastDeletedCount;

    public ExecutionArchiveStatsVO stats() {
        return ExecutionArchiveStatsVO.builder()
                .hotExecutions(executionMapper.countAll())
                .archivedExecutions(executionMapper.countArchived())
                .retentionDays(properties.getRetentionDays())
                .archiveEnabled(properties.isEnabled())
                .lastRunAt(lastRunAt)
                .lastArchivedCount(lastArchivedCount)
                .lastDeletedCount(lastDeletedCount)
                .build();
    }

    public ExecutionArchiveStatsVO runNow() {
        int[] result = executionArchiveService.archiveOldExecutions();
        lastRunAt = LocalDateTime.now();
        lastArchivedCount = result[0];
        lastDeletedCount = result[1];
        return stats();
    }
}
