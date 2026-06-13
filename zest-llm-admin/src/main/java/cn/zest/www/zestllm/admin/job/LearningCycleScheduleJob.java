package cn.zest.www.zestllm.admin.job;

import cn.zest.www.zestllm.admin.config.LearningCycleProperties;
import cn.zest.www.zestllm.admin.service.LearningManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zest-llm.admin.learning-cycle", name = "schedule-enabled", havingValue = "true")
public class LearningCycleScheduleJob {

    private final LearningManageService learningManageService;
    private final LearningCycleProperties properties;

    @Scheduled(cron = "${zest-llm.admin.learning-cycle.cron:0 0 3 * * ?}")
    public void runLearningCycles() {
        int count = learningManageService.runScheduledCycles(properties.isDryRunOnSchedule());
        log.info("Scheduled learning cycle finished count={} dryRun={}", count, properties.isDryRunOnSchedule());
    }
}
