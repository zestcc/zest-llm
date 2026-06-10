package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileProbeDO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeCheckDiffVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeCompareVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeTrendPointVO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public List<AgentProfileProbeTrendPointVO> trend(String taskCode, int days, String profileVersion) {
        var task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        int windowDays = Math.min(Math.max(days, 1), 90);
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
        List<LlmAgentProfileProbeDO> rows = probeRepo.listSince(task.getId(), since, profileVersion);
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (LlmAgentProfileProbeDO row : rows) {
            LocalDate day = row.getCreatedAt().toLocalDate();
            long[] counts = buckets.computeIfAbsent(day, ignored -> new long[4]);
            switch (row.getOverallStatus()) {
                case "READY" -> counts[0]++;
                case "DEGRADED" -> counts[1]++;
                case "UNAVAILABLE" -> counts[2]++;
                default -> { }
            }
            counts[3]++;
        }
        return buckets.entrySet().stream()
                .map(entry -> AgentProfileProbeTrendPointVO.builder()
                        .date(entry.getKey().toString())
                        .ready(entry.getValue()[0])
                        .degraded(entry.getValue()[1])
                        .unavailable(entry.getValue()[2])
                        .total(entry.getValue()[3])
                        .build())
                .toList();
    }

    public AgentProfileProbeCompareVO compareVersions(String taskCode, String fromVersion, String toVersion) {
        var task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        AgentProfileProbeResultVO from = probeRepo.findLatestByTaskIdAndVersion(task.getId(), fromVersion)
                .map(this::toResultVO)
                .orElseThrow(() -> new BusinessException("PROBE_NOT_FOUND", "版本无探测记录: " + fromVersion));
        AgentProfileProbeResultVO to = probeRepo.findLatestByTaskIdAndVersion(task.getId(), toVersion)
                .map(this::toResultVO)
                .orElseThrow(() -> new BusinessException("PROBE_NOT_FOUND", "版本无探测记录: " + toVersion));
        Map<String, AgentProfileProbeCheckVO> fromChecks = from.getChecks().stream()
                .collect(Collectors.toMap(AgentProfileProbeCheckVO::getName, c -> c, (a, b) -> a));
        Map<String, AgentProfileProbeCheckVO> toChecks = to.getChecks().stream()
                .collect(Collectors.toMap(AgentProfileProbeCheckVO::getName, c -> c, (a, b) -> a));
        List<String> names = java.util.stream.Stream.concat(fromChecks.keySet().stream(), toChecks.keySet().stream())
                .distinct()
                .sorted()
                .toList();
        List<AgentProfileProbeCheckDiffVO> diffs = names.stream().map(name -> {
            AgentProfileProbeCheckVO fc = fromChecks.get(name);
            AgentProfileProbeCheckVO tc = toChecks.get(name);
            String changeType = "UNCHANGED";
            if (fc == null) {
                changeType = "ADDED";
            } else if (tc == null) {
                changeType = "REMOVED";
            } else if (fc.isUp() != tc.isUp()) {
                changeType = fc.isUp() ? "REGRESSED" : "IMPROVED";
            }
            return AgentProfileProbeCheckDiffVO.builder()
                    .name(name)
                    .category(tc != null ? tc.getCategory() : fc != null ? fc.getCategory() : null)
                    .critical(tc != null ? tc.isCritical() : fc != null && fc.isCritical())
                    .fromUp(fc != null ? fc.isUp() : null)
                    .toUp(tc != null ? tc.isUp() : null)
                    .fromMessage(fc != null ? fc.getMessage() : null)
                    .toMessage(tc != null ? tc.getMessage() : null)
                    .changeType(changeType)
                    .build();
        }).toList();
        return AgentProfileProbeCompareVO.builder()
                .taskCode(taskCode)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .fromStatus(from.getOverallStatus())
                .toStatus(to.getOverallStatus())
                .diffs(diffs)
                .build();
    }

    public List<AgentProfileProbeResultVO> exportHistory(String taskCode, String profileVersion, int limit) {
        var task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        return probeRepo.listAllForExport(task.getId(), profileVersion, limit).stream()
                .map(this::toResultVO)
                .toList();
    }

    public String exportHistoryCsv(String taskCode, String profileVersion, int limit) {
        List<AgentProfileProbeResultVO> rows = exportHistory(taskCode, profileVersion, limit);
        StringBuilder sb = new StringBuilder("probeId,taskCode,profileVersion,overallStatus,latencyMs,probeSource,probedAt\n");
        for (AgentProfileProbeResultVO row : rows) {
            sb.append(row.getProbeId()).append(',')
                    .append(csv(row.getTaskCode())).append(',')
                    .append(csv(row.getProfileVersion())).append(',')
                    .append(csv(row.getOverallStatus())).append(',')
                    .append(row.getLatencyMs() != null ? row.getLatencyMs() : "").append(',')
                    .append(csv(row.getProbeSource())).append(',')
                    .append(row.getProbedAt() != null ? row.getProbedAt() : "")
                    .append('\n');
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
