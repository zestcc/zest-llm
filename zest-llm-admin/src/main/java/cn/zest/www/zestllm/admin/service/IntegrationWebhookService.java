package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.IntegrationWebhookProperties;
import cn.zest.www.zestllm.admin.model.entity.LlmIntegrationWebhookDeliveryDO;
import cn.zest.www.zestllm.admin.repo.LlmIntegrationWebhookDeliveryRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationWebhookService {

    private final IntegrationWebhookProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final LlmIntegrationWebhookDeliveryRepo deliveryRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPublishResult(String taskCode, String version, boolean success, String message, String operator) {
        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            return;
        }
        Map<String, Object> body = Map.of(
                "event", success ? "PROFILE_PUBLISH_SUCCESS" : "PROFILE_PUBLISH_FAILED",
                "taskCode", taskCode,
                "version", version,
                "success", success,
                "message", message != null ? message : "",
                "operator", operator != null ? operator : "admin"
        );
        deliverWithRetry(body, taskCode, version, (String) body.get("event"), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void redeliver(LlmIntegrationWebhookDeliveryDO row, Map<String, Object> body) {
        deliverWithRetry(body, row.getTaskCode(), row.getProfileVersion(), row.getEventType(), row);
    }

    private void deliverWithRetry(Map<String, Object> body, String taskCode, String version,
                                  String eventType, LlmIntegrationWebhookDeliveryDO existing) {
        int maxAttempts = Math.max(1, properties.getWebhookMaxRetries() + 1);
        long delayMs = Math.max(100L, properties.getWebhookRetryDelayMs());
        String payloadJson;
        String payloadHash;
        try {
            payloadJson = objectMapper.writeValueAsString(body);
            payloadHash = sha256(payloadJson);
        } catch (Exception ex) {
            log.warn("Integration webhook payload serialize failed taskCode={}", taskCode, ex);
            return;
        }

        LlmIntegrationWebhookDeliveryDO row = existing;
        if (row == null) {
            row = newDelivery(eventType, taskCode, version, properties.getWebhookUrl(), payloadHash, payloadJson, maxAttempts);
        } else {
            row.setStatus("PENDING");
            row.setDeadLetter(false);
            row.setMaxAttempts(maxAttempts);
            row.setDetailJson(payloadJson);
            row.setPayloadHash(payloadHash);
            row.setUpdatedAt(LocalDateTime.now());
            deliveryRepo.updateById(row);
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClientBuilder.build()
                        .post()
                        .uri(properties.getWebhookUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payloadJson)
                        .retrieve()
                        .toBodilessEntity();
                markSent(row, attempt);
                log.info("Integration webhook sent taskCode={} attempt={}", taskCode, attempt);
                return;
            } catch (Exception ex) {
                String lastError = ex.getMessage();
                markAttempt(row, attempt, lastError);
                if (attempt >= maxAttempts) {
                    markFailed(row, attempt, lastError, true);
                    log.warn("Integration webhook failed taskCode={} after {} attempts", taskCode, attempt, ex);
                    return;
                }
                log.debug("Integration webhook retry taskCode={} attempt={}", taskCode, attempt);
                try {
                    Thread.sleep(delayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    markFailed(row, attempt, "interrupted", true);
                    log.warn("Integration webhook interrupted taskCode={}", taskCode);
                    return;
                }
            }
        }
    }

    private LlmIntegrationWebhookDeliveryDO newDelivery(String eventType, String taskCode, String version,
                                                        String webhookUrl, String payloadHash, String detailJson,
                                                        int maxAttempts) {
        LlmIntegrationWebhookDeliveryDO row = new LlmIntegrationWebhookDeliveryDO();
        row.setEventType(eventType);
        row.setTaskCode(taskCode);
        row.setProfileVersion(version);
        row.setWebhookUrl(webhookUrl);
        row.setPayloadHash(payloadHash);
        row.setStatus("PENDING");
        row.setAttemptCount(0);
        row.setMaxAttempts(maxAttempts);
        row.setDeadLetter(false);
        row.setDetailJson(detailJson);
        LocalDateTime now = LocalDateTime.now();
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        deliveryRepo.insert(row);
        return row;
    }

    private void markAttempt(LlmIntegrationWebhookDeliveryDO row, int attempt, String lastError) {
        row.setAttemptCount(attempt);
        row.setLastError(lastError);
        row.setUpdatedAt(LocalDateTime.now());
        deliveryRepo.updateById(row);
    }

    private void markSent(LlmIntegrationWebhookDeliveryDO row, int attempt) {
        row.setStatus("SENT");
        row.setAttemptCount(attempt);
        row.setDeadLetter(false);
        row.setLastError(null);
        row.setUpdatedAt(LocalDateTime.now());
        deliveryRepo.updateById(row);
    }

    private void markFailed(LlmIntegrationWebhookDeliveryDO row, int attempt, String lastError, boolean deadLetter) {
        row.setStatus("FAILED");
        row.setAttemptCount(attempt);
        row.setLastError(lastError);
        row.setDeadLetter(deadLetter);
        row.setUpdatedAt(LocalDateTime.now());
        deliveryRepo.updateById(row);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return "";
        }
    }
}
