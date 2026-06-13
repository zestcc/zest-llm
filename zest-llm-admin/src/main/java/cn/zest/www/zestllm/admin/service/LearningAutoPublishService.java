package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.LearningAutoPublishProperties;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningAutoPublishService {

    private final LearningAutoPublishProperties properties;
    private final AgentProfilePublishService publishService;

    public boolean shouldAutoPublish(LearningCycleResult result, boolean dryRun) {
        if (!properties.isEnabled() || dryRun || result == null) {
            return false;
        }
        return result.isPublishAllowed();
    }

    public void tryAutoPublish(String taskCode, String profileVersion, LearningCycleResult result, boolean dryRun) {
        if (!shouldAutoPublish(result, dryRun)) {
            return;
        }
        try {
            publishService.publish(taskCode, profileVersion, "learning-auto-publish");
            log.info("Auto-published profile after learning cycle taskCode={} version={}", taskCode, profileVersion);
        } catch (Exception ex) {
            log.warn("Learning auto-publish failed taskCode={} version={}: {}",
                    taskCode, profileVersion, ex.getMessage());
        }
    }
}
