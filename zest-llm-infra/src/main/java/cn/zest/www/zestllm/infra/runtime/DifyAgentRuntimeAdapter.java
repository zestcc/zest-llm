package cn.zest.www.zestllm.infra.runtime;

import cn.zest.www.zestllm.infra.config.DifyProperties;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.profile.RuntimeBackendConfig;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeInvokeRequest;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Dify Agent Runtime（对标 Dify Chat API / Workflow API）。
 */
@Slf4j
@RequiredArgsConstructor
public class DifyAgentRuntimeAdapter implements AgentRuntimeAdapter {

    private final DifyProperties properties;
    private final SecretResolver secretResolver;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "dify";
    }

    @Override
    public ChatResponse invoke(AgentRuntimeInvokeRequest request) {
        RuntimeBackendConfig backend = request.getRuntimeBackend();
        String baseUrl = resolveBaseUrl(backend);
        String apiKey = resolveApiKey(backend);
        long start = System.currentTimeMillis();
        try {
            RestClient client = buildClient(baseUrl, apiKey, backend);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("query", request.getUserMessage() != null ? request.getUserMessage() : "ping");
            body.put("response_mode", "blocking");
            body.put("user", request.getTaskCode() != null ? request.getTaskCode() : "zestllm");
            if (StringUtils.hasText(backend.getExternalAppId())) {
                body.put("conversation_id", "");
            }
            String uri = StringUtils.hasText(backend.getExternalAppId())
                    ? "/v1/chat-messages"
                    : "/v1/chat-messages";
            ObjectNode headers = objectMapper.createObjectNode();
            if (StringUtils.hasText(backend.getExternalAppId())) {
                // Dify app-specific endpoint uses Authorization + app in URL for some versions
            }
            String raw = client.post()
                    .uri(uri)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildChatBody(request, backend).toString())
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(raw != null ? raw : "{}");
            String answer = node.path("answer").asText("");
            if (!StringUtils.hasText(answer)) {
                answer = node.path("data").path("answer").asText("");
            }
            return ChatResponse.builder()
                    .content(answer)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception ex) {
            log.warn("Dify invoke failed traceId={}", request.getTraceId(), ex);
            throw new IllegalStateException("Dify invoke failed: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode buildChatBody(AgentRuntimeInvokeRequest request, RuntimeBackendConfig backend) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("inputs", objectMapper.createObjectNode());
        body.put("query", request.getUserMessage() != null ? request.getUserMessage() : "ping");
        body.put("response_mode", "blocking");
        body.put("user", "zest-" + (request.getTaskCode() != null ? request.getTaskCode() : "probe"));
        return body;
    }

    @Override
    public HealthStatus health() {
        try {
            RestClient client = buildClient(properties.getBaseUrl(), properties.getApiKey(), null);
            client.get().uri("/health").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("Dify reachable").build();
        } catch (Exception first) {
            try {
                RestClient client = buildClient(properties.getBaseUrl(), properties.getApiKey(), null);
                client.get().uri("/v1/parameters").retrieve().toBodilessEntity();
                return HealthStatus.builder().up(true).message("Dify API reachable").build();
            } catch (Exception second) {
                return HealthStatus.builder().up(false).message("Dify unreachable: " + first.getMessage()).build();
            }
        }
    }

    public HealthStatus health(RuntimeBackendConfig backend) {
        if (backend == null) {
            return health();
        }
        try {
            RestClient client = buildClient(resolveBaseUrl(backend), resolveApiKey(backend), backend);
            client.get().uri("/health").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("Dify " + backend.getBaseUrl() + " OK").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message("Dify health failed: " + ex.getMessage()).build();
        }
    }

    private String resolveBaseUrl(RuntimeBackendConfig backend) {
        if (backend != null && StringUtils.hasText(backend.getBaseUrl())) {
            return backend.getBaseUrl();
        }
        return properties.getBaseUrl();
    }

    private String resolveApiKey(RuntimeBackendConfig backend) {
        if (backend != null && StringUtils.hasText(backend.getSecretRef())) {
            return secretResolver.resolve(backend.getSecretRef()).orElse(properties.getApiKey());
        }
        return properties.getApiKey();
    }

    private RestClient buildClient(String baseUrl, String apiKey, RuntimeBackendConfig backend) {
        int connectMs = backend != null && backend.getTimeoutMs() != null
                ? Math.min(backend.getTimeoutMs(), 10000) : properties.getConnectTimeoutMs();
        int readMs = backend != null && backend.getTimeoutMs() != null
                ? backend.getTimeoutMs() : properties.getReadTimeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectMs));
        factory.setReadTimeout(Duration.ofMillis(readMs));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(apiKey)) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }
}
