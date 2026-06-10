package cn.zest.www.zestllm.admin.job;

import cn.zest.www.zestllm.admin.config.AgentProfileProbeProperties;
import cn.zest.www.zestllm.admin.service.AgentProfileProbeRecordService;
import cn.zest.www.zestllm.admin.service.AgentProfileProbeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zest-llm.admin.agent-probe", name = "schedule-enabled", havingValue = "true", matchIfMissing = true)
public class AgentProfileProbeScheduleJob {

    private final AgentProfileProbeService agentProfileProbeService;
    private final AgentProfileProbeProperties properties;

    @Scheduled(cron = "${zest-llm.admin.agent-probe.cron:0 */30 * * * ?}")
    public void probePublishedProfiles() {
        int count = agentProfileProbeService.probeAllPublished(
                properties.isSmokeTestOnSchedule(),
                AgentProfileProbeRecordService.SOURCE_SCHEDULED);
        log.info("Scheduled agent profile probe finished count={}", count);
    }
}
