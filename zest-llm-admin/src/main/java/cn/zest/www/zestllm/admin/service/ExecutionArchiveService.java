package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.ExecutionArchiveProperties;
import cn.zest.www.zestllm.admin.mapper.LlmExecutionArchiveMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmExecutionArchiveRunDO;
import cn.zest.www.zestllm.admin.repo.LlmExecutionArchiveRunRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionArchiveService {

    private final ExecutionArchiveProperties properties;
    private final LlmExecutionArchiveMapper archiveMapper;
    private final LlmExecutionArchiveRunRepo archiveRunRepo;

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    @Transactional(rollbackFor = Exception.class)
    public int[] archiveOldExecutions() {
        return archiveOldExecutions(TRIGGER_SCHEDULED);
    }

    @Transactional(rollbackFor = Exception.class)
    public int[] archiveOldExecutions(String triggerSource) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(properties.getRetentionDays());
        int archived = archiveMapper.archiveBefore(cutoff);
        int deleted = archiveMapper.deleteBefore(cutoff);
        if (archived > 0 || deleted > 0) {
            log.info("Execution archive completed: archived={}, deleted={}, cutoff={}", archived, deleted, cutoff);
        }
        LlmExecutionArchiveRunDO run = new LlmExecutionArchiveRunDO();
        run.setArchivedCount(archived);
        run.setDeletedCount(deleted);
        run.setTriggerSource(triggerSource);
        run.setCreatedAt(LocalDateTime.now());
        archiveRunRepo.insert(run);
        return new int[]{archived, deleted};
    }
}
