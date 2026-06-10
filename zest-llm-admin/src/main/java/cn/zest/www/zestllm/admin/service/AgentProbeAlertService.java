package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.config.AgentProfileProbeProperties;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProbeAlertDO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProbeAlertVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProbeAlertRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProbeAlertService {

    private final AgentProfileProbeProperties properties;
    private final LlmAgentProbeAlertRepo probeAlertRepo;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Transactional(rollbackFor = Exception.class)
    public void notifyIfNeeded(Long taskId, AgentProfileProbeResultVO result) {
        if (result == null || !StringUtils.hasText(properties.getAlertWebhookUrl())) {
            return;
        }
        if ("READY".equals(result.getOverallStatus())) {
            return;
        }
        if ("DEGRADED".equals(result.getOverallStatus()) && !properties.isAlertOnDegraded()) {
            return;
        }
        if (!"UNAVAILABLE".equals(result.getOverallStatus()) && !"DEGRADED".equals(result.getOverallStatus())) {
            return;
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(Math.max(1, properties.getAlertCooldownMinutes()));
        if (probeAlertRepo.findRecentAlert(taskId, result.getOverallStatus(), since).isPresent()) {
            return;
        }

        String message = String.format("Agent profile probe %s for task %s version %s",
                result.getOverallStatus(), result.getTaskCode(), result.getProfileVersion());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "AGENT_PROBE");
        payload.put("taskCode", result.getTaskCode());
        payload.put("profileVersion", result.getProfileVersion());
        payload.put("overallStatus", result.getOverallStatus());
        payload.put("ready", result.isReady());
        payload.put("probeSource", result.getProbeSource());
        payload.put("probeId", result.getProbeId());
        payload.put("latencyMs", result.getLatencyMs());
        payload.put("message", message);

        String status = "SENT";
        try {
            restClientBuilder.build()
                    .post()
                    .uri(properties.getAlertWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(payload))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Agent probe alert webhook sent taskCode={} status={}", result.getTaskCode(), result.getOverallStatus());
        } catch (Exception ex) {
            status = "FAILED";
            log.warn("Failed to send agent probe alert taskCode={}", result.getTaskCode(), ex);
        }

        LlmAgentProbeAlertDO row = new LlmAgentProbeAlertDO();
        row.setTaskId(taskId);
        row.setTaskCode(result.getTaskCode());
        row.setProfileVersion(result.getProfileVersion());
        row.setOverallStatus(result.getOverallStatus());
        row.setProbeId(result.getProbeId());
        row.setWebhookUrl(properties.getAlertWebhookUrl());
        row.setStatus(status);
        row.setDetailJson(toJson(payload));
        row.setCreatedAt(LocalDateTime.now());
        probeAlertRepo.insert(row);
    }

    public List<AgentProbeAlertVO> listRecent(String taskCode, int limit) {
        return page(taskCode, 1, limit).getRecords();
    }

    public Page<AgentProbeAlertVO> page(String taskCode, int pageNum, int pageSize) {
        Page<LlmAgentProbeAlertDO> pager = probeAlertRepo.page(taskCode, pageNum, pageSize);
        Page<AgentProbeAlertVO> result = new Page<>(pager.getCurrent(), pager.getSize(), pager.getTotal());
        result.setRecords(pager.getRecords().stream()
                .map(row -> AgentProbeAlertVO.builder()
                        .id(row.getId())
                        .taskCode(row.getTaskCode())
                        .profileVersion(row.getProfileVersion())
                        .overallStatus(row.getOverallStatus())
                        .probeId(row.getProbeId())
                        .status(row.getStatus())
                        .message(extractMessage(row.getDetailJson()))
                        .createdAt(row.getCreatedAt())
                        .build())
                .toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentProbeAlertVO resend(Long alertId) {
        LlmAgentProbeAlertDO row = probeAlertRepo.findById(alertId)
                .orElseThrow(() -> new BusinessException("ALERT_NOT_FOUND", "告警记录不存在: " + alertId));
        if (!StringUtils.hasText(row.getWebhookUrl())) {
            throw new BusinessException("WEBHOOK_NOT_CONFIGURED", "未配置 Webhook URL");
        }
        String status = "SENT";
        try {
            restClientBuilder.build()
                    .post()
                    .uri(row.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(row.getDetailJson())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            status = "FAILED";
            log.warn("Failed to resend agent probe alert id={}", alertId, ex);
        }
        row.setStatus(status);
        row.setCreatedAt(LocalDateTime.now());
        probeAlertRepo.updateById(row);
        return AgentProbeAlertVO.builder()
                .id(row.getId())
                .taskCode(row.getTaskCode())
                .profileVersion(row.getProfileVersion())
                .overallStatus(row.getOverallStatus())
                .probeId(row.getProbeId())
                .status(row.getStatus())
                .message(extractMessage(row.getDetailJson()))
                .createdAt(row.getCreatedAt())
                .build();
    }

    private String extractMessage(String detailJson) {
        if (!StringUtils.hasText(detailJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(detailJson).path("message").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
