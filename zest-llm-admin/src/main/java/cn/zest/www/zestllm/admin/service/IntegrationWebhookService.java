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
        try {
            Map<String, Object> body = Map.of(
                    "event", success ? "PROFILE_PUBLISH_SUCCESS" : "PROFILE_PUBLISH_FAILED",
                    "taskCode", taskCode,
                    "version", version,
                    "success", success,
                    "message", message != null ? message : "",
                    "operator", operator != null ? operator : "admin"
            );
            restClientBuilder.build()
                    .post()
                    .uri(properties.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Integration webhook sent taskCode={} success={}", taskCode, success);
        } catch (Exception ex) {
            log.warn("Integration webhook failed taskCode={}", taskCode, ex);
        }
    }
}
