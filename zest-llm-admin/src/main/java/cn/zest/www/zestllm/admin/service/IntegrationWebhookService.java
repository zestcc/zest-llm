package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.IntegrationWebhookProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationWebhookService {

    private final IntegrationWebhookProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

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
        deliverWithRetry(body, taskCode);
    }

    private void deliverWithRetry(Map<String, Object> body, String taskCode) {
        int maxAttempts = Math.max(1, properties.getWebhookMaxRetries() + 1);
        long delayMs = Math.max(100L, properties.getWebhookRetryDelayMs());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClientBuilder.build()
                        .post()
                        .uri(properties.getWebhookUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .toBodilessEntity();
                log.info("Integration webhook sent taskCode={} attempt={}", taskCode, attempt);
                return;
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    log.warn("Integration webhook failed taskCode={} after {} attempts", taskCode, attempt, ex);
                    return;
                }
                log.debug("Integration webhook retry taskCode={} attempt={}", taskCode, attempt);
                try {
                    Thread.sleep(delayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Integration webhook interrupted taskCode={}", taskCode);
                    return;
                }
            }
        }
    }
}
