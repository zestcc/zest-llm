package cn.zest.www.zestllm.infra.alert;

import cn.zest.www.zestllm.spi.alert.AlertWebhookAdapter;
import cn.zest.www.zestllm.spi.alert.CostAlertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class HttpAlertWebhookAdapter implements AlertWebhookAdapter {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "http";
    }

    @Override
    public void send(String webhookUrl, CostAlertEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "type", "COST_THRESHOLD",
                    "appKey", event.getAppKey(),
                    "alertDate", event.getAlertDate().toString(),
                    "dailyCost", event.getDailyCost(),
                    "costLimit", event.getCostLimit(),
                    "thresholdPct", event.getThresholdPct(),
                    "message", event.getMessage()
            );
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Cost alert webhook sent appKey={} url={}", event.getAppKey(), webhookUrl);
        } catch (Exception ex) {
            log.warn("Failed to send cost alert webhook appKey={}", event.getAppKey(), ex);
        }
    }
}
