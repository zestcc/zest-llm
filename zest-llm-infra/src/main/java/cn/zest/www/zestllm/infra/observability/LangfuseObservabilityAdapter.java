package cn.zest.www.zestllm.infra.observability;

import cn.zest.www.zestllm.infra.config.LangfuseProperties;
import cn.zest.www.zestllm.spi.model.TraceEndEvent;
import cn.zest.www.zestllm.spi.model.TraceStartEvent;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
public class LangfuseObservabilityAdapter implements ObservabilityAdapter {

    private final LangfuseProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "langfuse-ingest");
        t.setDaemon(true);
        return t;
    });

    @Override
    public String adapterId() {
        return "langfuse";
    }

    @Override
    public void traceStart(TraceStartEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        executor.submit(() -> ingest(event.getTraceId(), "trace-create", event.getAppKey(),
                event.getCode(), event.getModel(), null, null, null, null));
    }

    @Override
    public void traceEnd(TraceEndEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        executor.submit(() -> ingest(event.getTraceId(), "trace-update", null, null, null,
                event.isSuccess(), event.getPromptTokens(), event.getCompletionTokens(), event.getLatencyMs()));
    }

    private void ingest(String traceId, String type, String appKey, String code, String model,
                        Boolean success, Integer promptTokens, Integer completionTokens, Long latencyMs) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode batch = body.putArray("batch");
            ObjectNode item = batch.addObject();
            item.put("type", type);
            item.put("id", traceId + "-" + System.nanoTime());
            ObjectNode payload = item.putObject(type.equals("trace-create") ? "body" : "body");
            payload.put("id", traceId);
            payload.put("name", code != null ? code : "llm-execution");
            if (appKey != null) {
                payload.put("userId", appKey);
            }
            if (model != null) {
                payload.put("metadata", model);
            }
            if (success != null) {
                payload.put("level", success ? "DEFAULT" : "ERROR");
            }
            if (latencyMs != null) {
                payload.put("latencyMs", latencyMs);
            }
            if (promptTokens != null) {
                payload.put("inputTokens", promptTokens);
            }
            if (completionTokens != null) {
                payload.put("outputTokens", completionTokens);
            }

            restClient.post()
                    .uri("/api/public/ingestion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuth())
                    .body(body.toString())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.debug("Langfuse ingest skipped traceId={} reason={}", traceId, ex.getMessage());
        }
    }

    private String basicAuth() {
        String raw = properties.getPublicKey() + ":" + properties.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
