package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.ExecutionArchiveProperties;
import cn.zest.www.zestllm.admin.mapper.LlmExecutionMapper;
import cn.zest.www.zestllm.admin.model.vo.ExecutionArchiveStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecutionArchiveAdminService {

    private final ExecutionArchiveProperties properties;
    private final LlmExecutionMapper executionMapper;
    private final ExecutionArchiveService executionArchiveService;

    public ExecutionArchiveStatsVO stats() {
        return ExecutionArchiveStatsVO.builder()
                .hotExecutions(executionMapper.countAll())
                .archivedExecutions(executionMapper.countArchived())
                .retentionDays(properties.getRetentionDays())
                .archiveEnabled(properties.isEnabled())
                .build();
    }

    public ExecutionArchiveStatsVO runNow() {
        executionArchiveService.archiveOldExecutions();
        return stats();
    }
}
