package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.ExecutionArchiveProperties;
import cn.zest.www.zestllm.admin.mapper.LlmExecutionMapper;
import cn.zest.www.zestllm.admin.model.vo.ExecutionArchiveRunVO;
import cn.zest.www.zestllm.admin.model.vo.ExecutionArchiveStatsVO;
import cn.zest.www.zestllm.admin.repo.LlmExecutionArchiveRunRepo;
import cn.zest.www.zestllm.admin.model.entity.LlmExecutionArchiveRunDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutionArchiveAdminService {

    private final ExecutionArchiveProperties properties;
    private final LlmExecutionMapper executionMapper;
    private final ExecutionArchiveService executionArchiveService;
    private final LlmExecutionArchiveRunRepo archiveRunRepo;

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
        int[] result = executionArchiveService.archiveOldExecutions(ExecutionArchiveService.TRIGGER_MANUAL);
        lastRunAt = LocalDateTime.now();
        lastArchivedCount = result[0];
        lastDeletedCount = result[1];
        return stats();
    }

    public Page<ExecutionArchiveRunVO> pageRuns(int pageNum, int pageSize) {
        Page<LlmExecutionArchiveRunDO> pager = archiveRunRepo.page(pageNum, pageSize);
        Page<ExecutionArchiveRunVO> result = new Page<>(pager.getCurrent(), pager.getSize(), pager.getTotal());
        result.setRecords(pager.getRecords().stream()
                .map(row -> ExecutionArchiveRunVO.builder()
                        .id(row.getId())
                        .archivedCount(row.getArchivedCount())
                        .deletedCount(row.getDeletedCount())
                        .triggerSource(row.getTriggerSource())
                        .createdAt(row.getCreatedAt())
                        .build())
                .toList());
        return result;
    }
}
