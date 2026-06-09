package cn.zest.www.zestllm.infra.gateway;

import cn.zest.www.zestllm.infra.config.LiteLLMProperties;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.model.ChatRequest;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class LiteLLMGatewayAdapter implements ModelGatewayAdapter {

    private final LiteLLMProperties properties;
    private final ObjectMapper objectMapper;
    private RestClient restClient;

    private RestClient client() {
        if (restClient == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
            factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(factory);
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                builder.defaultHeader("Authorization", "Bearer " + properties.getApiKey());
            }
            restClient = builder.build();
        }
        return restClient;
    }

    @Override
    public String adapterId() {
        return "litellm";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        List<String> models = new ArrayList<>();
        models.add(request.getModel());
        if (request.getFallbackModels() != null) {
            models.addAll(request.getFallbackModels());
        }
        RestClientException lastError = null;
        for (String model : models) {
            if (model == null || model.isBlank()) {
                continue;
            }
            try {
                return doChat(request, model, start);
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("LiteLLM model failed model={} traceId={}", model, request.getTraceId(), ex);
            }
        }
        throw lastError != null ? lastError : new RestClientException("No model configured");
    }

    private ChatResponse doChat(ChatRequest request, String model, long start) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        ArrayNode messages = objectMapper.createArrayNode();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", request.getSystemPrompt()));
        }
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", request.getUserMessage()));
        body.set("messages", messages);

        String raw = client().post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode usage = root.path("usage");
            return ChatResponse.builder()
                    .model(model)
                    .content(content)
                    .promptTokens(usage.path("prompt_tokens").asInt(0))
                    .completionTokens(usage.path("completion_tokens").asInt(0))
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (JsonProcessingException ex) {
            throw new RestClientException("Invalid LiteLLM response", ex);
        }
    }

    @Override
    public HealthStatus health() {
        try {
            client().get().uri("/health/liveliness").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("ok").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message(ex.getMessage()).build();
        }
    }
}
