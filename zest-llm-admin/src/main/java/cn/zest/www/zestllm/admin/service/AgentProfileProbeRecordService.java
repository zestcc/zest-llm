package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileProbeDO;
import cn.zest.www.zestllm.admin.model.vo.AgentHealthDashboardVO;
import cn.zest.www.zestllm.admin.model.vo.AgentHealthItemVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeCheckVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileProbeRepo;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProfileProbeRecordService {

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_SCHEDULED = "SCHEDULED";

    private final LlmAgentProfileProbeRepo probeRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final ObjectMapper objectMapper;
    private final AgentProbeAlertService agentProbeAlertService;

    @Transactional(rollbackFor = Exception.class)
    public AgentProfileProbeResultVO save(Long taskId, AgentProfileProbeResultVO result, boolean smokeTest, String probeSource) {
        LlmAgentProfileProbeDO entity = new LlmAgentProfileProbeDO();
        entity.setTaskId(taskId);
        entity.setTaskCode(result.getTaskCode());
        entity.setProfileVersion(result.getProfileVersion());
        entity.setProfileStatus(result.getProfileStatus());
        entity.setOverallStatus(result.getOverallStatus());
        entity.setReady(result.isReady());
        entity.setSmokeTest(smokeTest);
        entity.setProbeSource(probeSource);
        entity.setLatencyMs(result.getLatencyMs());
        entity.setChecksJson(toJson(result.getChecks()));
        entity.setCreatedAt(LocalDateTime.now());
        probeRepo.insert(entity);

        result.setProbeId(entity.getId());
        result.setProbeSource(probeSource);
        result.setProbedAt(entity.getCreatedAt());
        agentProbeAlertService.notifyIfNeeded(taskId, result);
        return result;
    }

    public Optional<AgentProfileProbeResultVO> latest(String taskCode) {
        return taskDefRepo.findByCode(taskCode)
                .flatMap(task -> probeRepo.findLatestByTaskId(task.getId()))
                .map(this::toResultVO);
    }

    public Page<AgentProfileProbeResultVO> history(String taskCode, int page, int size, String profileVersion) {
        var task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        Page<LlmAgentProfileProbeDO> pager = probeRepo.pageByTaskId(task.getId(), page, size, profileVersion);
        Page<AgentProfileProbeResultVO> result = new Page<>(pager.getCurrent(), pager.getSize(), pager.getTotal());
        result.setRecords(pager.getRecords().stream().map(this::toResultVO).toList());
        return result;
    }

    public AgentHealthDashboardVO dashboardHealth() {
        long monitored = agentProfileRepo.countPublishedTasks();
        long ready = probeRepo.countLatestByStatus("READY");
        long degraded = probeRepo.countLatestByStatus("DEGRADED");
        long unavailable = probeRepo.countLatestByStatus("UNAVAILABLE");
        long probedTasks = probeRepo.findLatestPerTask().size();
        long unknown = Math.max(0, monitored - probedTasks);

        List<AgentHealthItemVO> alerts = probeRepo.findLatestPerTask().stream()
                .filter(p -> !"READY".equals(p.getOverallStatus()))
                .sorted(Comparator.comparing(LlmAgentProfileProbeDO::getCreatedAt).reversed())
                .map(this::toHealthItem)
                .limit(20)
                .toList();

        return AgentHealthDashboardVO.builder()
                .monitored(monitored)
                .ready(ready)
                .degraded(degraded)
                .unavailable(unavailable)
                .unknown(unknown)
                .alerts(alerts)
                .build();
    }

    private AgentHealthItemVO toHealthItem(LlmAgentProfileProbeDO entity) {
        return AgentHealthItemVO.builder()
                .taskCode(entity.getTaskCode())
                .profileVersion(entity.getProfileVersion())
                .overallStatus(entity.getOverallStatus())
                .ready(Boolean.TRUE.equals(entity.getReady()))
                .latencyMs(entity.getLatencyMs())
                .probedAt(entity.getCreatedAt())
                .probeSource(entity.getProbeSource())
                .build();
    }

    private AgentProfileProbeResultVO toResultVO(LlmAgentProfileProbeDO entity) {
        return AgentProfileProbeResultVO.builder()
                .probeId(entity.getId())
                .taskCode(entity.getTaskCode())
                .profileVersion(entity.getProfileVersion())
                .profileStatus(entity.getProfileStatus())
                .overallStatus(entity.getOverallStatus())
                .ready(Boolean.TRUE.equals(entity.getReady()))
                .latencyMs(entity.getLatencyMs())
                .probeSource(entity.getProbeSource())
                .probedAt(entity.getCreatedAt())
                .checks(fromJson(entity.getChecksJson()))
                .build();
    }

    private String toJson(List<AgentProfileProbeCheckVO> checks) {
        try {
            return objectMapper.writeValueAsString(checks);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize probe checks", ex);
            return "[]";
        }
    }

    private List<AgentProfileProbeCheckVO> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize probe checks", ex);
            return List.of();
        }
    }
}
