package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.infra.config.LangfuseProperties;
import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
public class LangfuseTraceSampleService {

    private final LangfuseProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public LangfuseTraceSampleService(LangfuseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .build();
    }

    public List<EvalCaseSuggestion> suggestFromLangfuse(TraceSampleQuery query) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getPublicKey())) {
            return List.of();
        }
        int limit = query.getLimit() > 0 ? query.getLimit() : 20;
        try {
            String body = restClient.get()
                    .uri("/api/public/traces?limit={limit}", Math.min(limit * 3, 100))
                    .header("Authorization", basicAuth())
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(body)) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode rows = root.has("data") ? root.get("data") : root;
            if (!rows.isArray()) {
                return List.of();
            }
            LocalDateTime since = query.getSince() != null
                    ? LocalDateTime.ofInstant(query.getSince(), ZoneId.systemDefault())
                    : LocalDateTime.now().minusDays(7);
            List<EvalCaseSuggestion> suggestions = new ArrayList<>();
            for (Iterator<JsonNode> it = rows.elements(); it.hasNext(); ) {
                JsonNode trace = it.next();
                if (!matchesTask(trace, query.getTaskCode())) {
                    continue;
                }
                if (!isLowQuality(trace, since)) {
                    continue;
                }
                suggestions.add(EvalCaseSuggestion.builder()
                        .traceId(text(trace, "id"))
                        .suggestedInput(extractInput(trace))
                        .suggestedExpected("")
                        .reason(lowQualityReason(trace))
                        .source("langfuse:low_score")
                        .build());
                if (suggestions.size() >= limit) {
                    break;
                }
            }
            return suggestions;
        } catch (Exception ex) {
            log.debug("Langfuse trace sample skipped: {}", ex.getMessage());
            return List.of();
        }
    }

    private boolean matchesTask(JsonNode trace, String taskCode) {
        if (!StringUtils.hasText(taskCode)) {
            return true;
        }
        String name = text(trace, "name");
        if (taskCode.equals(name)) {
            return true;
        }
        JsonNode metadata = trace.get("metadata");
        if (metadata != null) {
            if (metadata.isTextual() && metadata.asText().contains(taskCode)) {
                return true;
            }
            if (metadata.isObject()) {
                JsonNode code = metadata.get("code");
                if (code != null && taskCode.equals(code.asText())) {
                    return true;
                }
            }
        }
        return name != null && name.contains(taskCode);
    }

    private boolean isLowQuality(JsonNode trace, LocalDateTime since) {
        Instant ts = parseInstant(text(trace, "timestamp"));
        if (ts != null && LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).isBefore(since)) {
            return false;
        }
        String level = text(trace, "level");
        if ("ERROR".equalsIgnoreCase(level)) {
            return true;
        }
        JsonNode scores = trace.get("scores");
        if (scores != null && scores.isArray()) {
            for (JsonNode score : scores) {
                double value = score.path("value").asDouble(1.0);
                if (value < 0.6) {
                    return true;
                }
            }
        }
        return false;
    }

    private String lowQualityReason(JsonNode trace) {
        String level = text(trace, "level");
        if ("ERROR".equalsIgnoreCase(level)) {
            return "langfuse trace ERROR";
        }
        return "langfuse low score";
    }

    private String extractInput(JsonNode trace) {
        JsonNode input = trace.get("input");
        if (input == null || input.isNull()) {
            return "";
        }
        return input.isTextual() ? input.asText() : input.toString();
    }

    private Instant parseInstant(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private String basicAuth() {
        String raw = properties.getPublicKey() + ":" + properties.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:3000";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
