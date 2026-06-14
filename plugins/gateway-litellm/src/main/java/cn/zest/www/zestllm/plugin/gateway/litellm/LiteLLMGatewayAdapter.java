package cn.zest.www.zestllm.plugin.gateway.litellm;

import cn.zest.www.zestllm.plugin.gateway.litellm.LiteLLMProperties;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class LiteLLMGatewayAdapter implements ModelGatewayAdapter {

    private final LiteLLMProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, RestClient> clients = new ConcurrentHashMap<>();

    @Override
    public String adapterId() {
        return "litellm";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        String protocol = resolveProtocol(request);
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
                return GatewayApiProtocol.isAnthropic(protocol)
                        ? doChatAnthropic(request, model, start)
                        : doChatOpenAi(request, model, start);
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("LiteLLM model failed model={} protocol={} traceId={}",
                        model, protocol, request.getTraceId(), ex);
            }
        }
        throw lastError != null ? lastError : new RestClientException("No model configured");
    }

    @Override
    public HealthStatus health() {
        try {
            defaultClient().get().uri("/health/liveliness").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("ok").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message(ex.getMessage()).build();
        }
    }

    String resolveProtocol(ChatRequest request) {
        return GatewayApiProtocol.normalize(
                StringUtils.hasText(request.getApiProtocol())
                        ? request.getApiProtocol()
                        : properties.getDefaultApiProtocol());
    }

    private RestClient buildClient(String baseUrl, String apiKey, String protocol) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);
        GatewayAuthApplier.applyToRestClient(builder, protocol, apiKey);
        return builder.build();
    }

    private RestClient clientFor(String baseUrl, String apiKey, String protocol) {
        String key = baseUrl + "\0" + protocol + "\0" + (apiKey != null ? apiKey : "");
        return clients.computeIfAbsent(key, ignored -> buildClient(baseUrl, apiKey, protocol));
    }

    private RestClient clientFor(ChatRequest request) {
        String baseUrl = StringUtils.hasText(request.getBaseUrl()) ? request.getBaseUrl() : properties.getBaseUrl();
        String apiKey = request.getApiKey() != null ? request.getApiKey() : properties.getApiKey();
        return clientFor(baseUrl, apiKey, resolveProtocol(request));
    }

    private RestClient defaultClient() {
        return clientFor(properties.getBaseUrl(), properties.getApiKey(),
                GatewayApiProtocol.normalize(properties.getDefaultApiProtocol()));
    }

    private ChatResponse doChatOpenAi(ChatRequest request, String model, long start) {
        ObjectNode body = buildOpenAiBody(request, model, false);
        String raw = clientFor(request).post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .body(String.class);
        return parseOpenAiResponse(raw, model, start);
    }

    private ChatResponse doChatAnthropic(ChatRequest request, String model, long start) {
        ObjectNode body = buildAnthropicBody(request, model, false);
        String raw = clientFor(request).post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("anthropic-version", "2023-06-01")
                .body(body.toString())
                .retrieve()
                .body(String.class);
        return parseAnthropicResponse(raw, model, start);
    }

    ObjectNode buildOpenAiBody(ChatRequest request, String model, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        if (stream) {
            body.put("stream", true);
        }
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
        return body;
    }

    ObjectNode buildAnthropicBody(ChatRequest request, String model, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        if (stream) {
            body.put("stream", true);
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            body.put("system", request.getSystemPrompt());
        }
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", request.getUserMessage()));
        body.set("messages", messages);
        return body;
    }

    private ChatResponse parseOpenAiResponse(String raw, String model, long start) {
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
            throw new RestClientException("Invalid LiteLLM OpenAI response", ex);
        }
    }

    private ChatResponse parseAnthropicResponse(String raw, String model, long start) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = GatewayApiProtocol.extractAnthropicText(root.path("content"));
            JsonNode usage = root.path("usage");
            return ChatResponse.builder()
                    .model(model)
                    .content(content)
                    .promptTokens(usage.path("input_tokens").asInt(0))
                    .completionTokens(usage.path("output_tokens").asInt(0))
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (JsonProcessingException ex) {
            throw new RestClientException("Invalid LiteLLM Anthropic response", ex);
        }
    }
}

