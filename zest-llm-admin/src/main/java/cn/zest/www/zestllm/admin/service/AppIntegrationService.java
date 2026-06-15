package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeCheckVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import cn.zest.www.zestllm.common.api.integration.AppIntegrationCheck;
import cn.zest.www.zestllm.common.api.integration.AppIntegrationStatusResponse;
import cn.zest.www.zestllm.common.api.integration.AppTaskAvailabilityResponse;
import cn.zest.www.zestllm.common.api.integration.AppTaskSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 第三方 App 集成查询（runtime token 鉴权，SSOT 由控制面聚合）。
 */
@Service
@RequiredArgsConstructor
public class AppIntegrationService {

    private final RuntimeAuthService runtimeAuthService;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmModelRouteRepo modelRouteRepo;
    private final AgentProfileProbeService agentProfileProbeService;
    private final AgentProfileProbeRecordService probeRecordService;

    public AppIntegrationStatusResponse getIntegrationStatus(String bearerToken, String appKey) {
        LlmAppDO app = authenticateApp(bearerToken, appKey);
        List<AppTaskSummary> tasks = listTaskSummaries(app);
        int readyCount = (int) tasks.stream().filter(AppTaskSummary::isReady).count();
        String overall = resolveOverallStatus(tasks);
        return AppIntegrationStatusResponse.builder()
                .appKey(app.getAppKey())
                .appName(app.getAppName())
                .appStatus(app.getStatus())
                .overallStatus(overall)
                .ready("READY".equals(overall))
                .taskCount(tasks.size())
                .readyTaskCount(readyCount)
                .tasks(tasks)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    public List<AppTaskSummary> listTasks(String bearerToken, String appKey) {
        LlmAppDO app = authenticateApp(bearerToken, appKey);
        return listTaskSummaries(app);
    }

    public AppTaskAvailabilityResponse getTaskAvailability(String bearerToken, String appKey, String taskCode,
                                                           boolean smokeTest) {
        LlmAppDO app = authenticateApp(bearerToken, appKey);
        LlmAiTaskDefDO task = requireTaskForApp(app, taskCode);
        AgentProfileProbeRequest request = new AgentProfileProbeRequest();
        request.setSmokeTest(smokeTest);
        request.setAppKey(app.getAppKey());
        AgentProfileProbeResultVO probe = agentProfileProbeService.probePublished(task.getCode(), request);
        return toAvailabilityResponse(app.getAppKey(), probe, smokeTest);
    }

    private LlmAppDO authenticateApp(String bearerToken, String appKey) {
        if (!StringUtils.hasText(appKey)) {
            throw new BusinessException("APP_KEY_REQUIRED", "appKey 不能为空");
        }
        return runtimeAuthService.authenticate(appKey.trim(), bearerToken);
    }

    private LlmAiTaskDefDO requireTaskForApp(LlmAppDO app, String taskCode) {
        if (!StringUtils.hasText(taskCode)) {
            throw new BusinessException("TASK_CODE_REQUIRED", "taskCode 不能为空");
        }
        return taskDefRepo.findByAppIdAndCode(app.getId(), taskCode.trim())
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在或不属于该应用: " + taskCode));
    }

    private List<AppTaskSummary> listTaskSummaries(LlmAppDO app) {
        List<LlmAiTaskDefDO> tasks = taskDefRepo.findByAppId(app.getId());
        tasks.sort(Comparator.comparing(LlmAiTaskDefDO::getCode, String.CASE_INSENSITIVE_ORDER));
        List<AppTaskSummary> summaries = new ArrayList<>();
        for (LlmAiTaskDefDO task : tasks) {
            summaries.add(buildTaskSummary(task));
        }
        return summaries;
    }

    private AppTaskSummary buildTaskSummary(LlmAiTaskDefDO task) {
        Optional<LlmAgentProfileDO> published = agentProfileRepo.findPublishedByTaskId(task.getId());
        Optional<LlmModelRouteDO> route = modelRouteRepo.findActiveByTaskId(task.getId());
        Optional<AgentProfileProbeResultVO> lastProbe = probeRecordService.latest(task.getCode());

        String overallStatus = "UNCONFIGURED";
        boolean ready = false;
        LocalDateTime lastProbedAt = null;
        if (lastProbe.isPresent()) {
            overallStatus = lastProbe.get().getOverallStatus();
            ready = lastProbe.get().isReady();
            lastProbedAt = lastProbe.get().getProbedAt();
        } else if (published.isPresent()) {
            overallStatus = "UNKNOWN";
        }

        return AppTaskSummary.builder()
                .taskCode(task.getCode())
                .taskName(task.getName())
                .taskStatus(task.getStatus())
                .publishedVersion(published.map(LlmAgentProfileDO::getVersion).orElse(null))
                .profileStatus(published.map(LlmAgentProfileDO::getStatus).orElse(null))
                .runtimeMode(published.map(LlmAgentProfileDO::getRuntimeMode).orElse(null))
                .providerPresetCode(published.map(LlmAgentProfileDO::getProviderPresetCode).orElse(null))
                .primaryModel(route.map(LlmModelRouteDO::getPrimaryModel).orElse(null))
                .overallStatus(overallStatus)
                .ready(ready)
                .lastProbedAt(lastProbedAt)
                .build();
    }

    private static String resolveOverallStatus(List<AppTaskSummary> tasks) {
        if (tasks.isEmpty()) {
            return "UNAVAILABLE";
        }
        boolean anyPublished = tasks.stream().anyMatch(t -> StringUtils.hasText(t.getPublishedVersion()));
        if (!anyPublished) {
            return "UNAVAILABLE";
        }
        boolean anyUnavailable = tasks.stream()
                .filter(t -> StringUtils.hasText(t.getPublishedVersion()))
                .anyMatch(t -> "UNAVAILABLE".equalsIgnoreCase(t.getOverallStatus())
                        || "UNCONFIGURED".equalsIgnoreCase(t.getOverallStatus()));
        if (anyUnavailable) {
            return "UNAVAILABLE";
        }
        boolean anyDegraded = tasks.stream()
                .anyMatch(t -> "DEGRADED".equalsIgnoreCase(t.getOverallStatus())
                        || "UNKNOWN".equalsIgnoreCase(t.getOverallStatus()));
        if (anyDegraded) {
            return "DEGRADED";
        }
        boolean allReady = tasks.stream()
                .filter(t -> StringUtils.hasText(t.getPublishedVersion()))
                .allMatch(AppTaskSummary::isReady);
        return allReady ? "READY" : "DEGRADED";
    }

    private AppTaskAvailabilityResponse toAvailabilityResponse(String appKey, AgentProfileProbeResultVO probe,
                                                               boolean smokeTest) {
        List<AppIntegrationCheck> checks = probe.getChecks() == null ? List.of()
                : probe.getChecks().stream().map(AppIntegrationService::toCheck).toList();
        return AppTaskAvailabilityResponse.builder()
                .appKey(appKey)
                .taskCode(probe.getTaskCode())
                .profileVersion(probe.getProfileVersion())
                .profileStatus(probe.getProfileStatus())
                .overallStatus(probe.getOverallStatus())
                .ready(probe.isReady())
                .latencyMs(probe.getLatencyMs())
                .smokeTest(smokeTest)
                .checks(checks)
                .probedAt(probe.getProbedAt() != null ? probe.getProbedAt() : LocalDateTime.now())
                .build();
    }

    private static AppIntegrationCheck toCheck(AgentProfileProbeCheckVO vo) {
        return AppIntegrationCheck.builder()
                .name(vo.getName())
                .category(vo.getCategory())
                .critical(vo.isCritical())
                .up(vo.isUp())
                .message(vo.getMessage())
                .build();
    }
}
