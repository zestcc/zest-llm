package cn.zest.www.zestllm.admin.job;

import cn.zest.www.zestllm.admin.service.ExecutionArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zest-llm.admin.execution-archive", name = "enabled", havingValue = "true")
public class ExecutionArchiveJob {

    private final ExecutionArchiveService executionArchiveService;

    @Scheduled(cron = "${zest-llm.admin.execution-archive.cron:0 0 3 * * ?}")
    public void archiveOldExecutions() {
        executionArchiveService.archiveOldExecutions();
    }
}
