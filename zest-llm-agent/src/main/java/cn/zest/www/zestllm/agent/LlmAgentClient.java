package cn.zest.www.zestllm.agent;

import cn.zest.www.zestllm.agent.cache.AgentPolicyCache;
import cn.zest.www.zestllm.agent.config.LlmAgentProperties;
import cn.zest.www.zestllm.common.api.StreamChunk;
import cn.zest.www.zestllm.infra.gateway.SseStreamHandler;
import cn.zest.www.zestllm.infra.guardrails.GuardrailsEnforcer;
import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.infra.tool.FunctionCallLoop;
import cn.zest.www.zestllm.infra.tool.ToolLoopParams;
import cn.zest.www.zestllm.infra.tool.ToolOrchestrator;
import cn.zest.www.zestllm.common.api.InvokeMetrics;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.api.PrepareRequest;
import cn.zest.www.zestllm.common.api.PrepareResponse;
import cn.zest.www.zestllm.common.api.ReportRequest;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;
import cn.zest.www.zestllm.spi.schema.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
public class LlmAgentClient {

    private static final String PREPARE_PATH = "/v1/llm/prepare";
    private static final String REPORT_PATH = "/v1/llm/report";

    private final LlmAgentProperties properties;
    private final RestClient controlPlaneClient;
    private final RestClient litellmClient;
    private final ObjectMapper objectMapper;
    private final OutputSchemaValidator outputSchemaValidator;
    private final ToolOrchestrator toolOrchestrator;
    private final FunctionCallLoop functionCallLoop;
    private final SseStreamHandler streamHandler;
    private final AgentPolicyCache policyCache;
    private final ContentModerationAdapter contentModerationAdapter;
    private final ExecutorService reportExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "llm-agent-report");
        t.setDaemon(true);
        return t;
    });

    public LlmAgentClient(LlmAgentProperties properties,
                          RestClient controlPlaneClient,
                          RestClient litellmClient,
                          ObjectMapper objectMapper,
                          OutputSchemaValidator outputSchemaValidator,
                          ToolOrchestrator toolOrchestrator,
                          FunctionCallLoop functionCallLoop,
                          SseStreamHandler streamHandler,
                          AgentPolicyCache policyCache,
                          ContentModerationAdapter contentModerationAdapter) {
        this.properties = properties;
        this.controlPlaneClient = controlPlaneClient;
        this.litellmClient = litellmClient;
        this.objectMapper = objectMapper;
        this.outputSchemaValidator = outputSchemaValidator;
        this.toolOrchestrator = toolOrchestrator;
        this.functionCallLoop = functionCallLoop;
        this.streamHandler = streamHandler;
        this.policyCache = policyCache;
        this.contentModerationAdapter = contentModerationAdapter;
    }

    public PrepareResponse prepare(InvokeRequest request) {
        fillDefaults(request);
        String appKey = request.getAppKey();
        String code = request.getCode();
        try {
            PrepareResponse response = fetchPrepareFromControlPlane(request);
            policyCache.put(appKey, code, response);
            return response;
        } catch (RestClientException ex) {
            log.warn("Control Plane prepare failed appKey={} code={}, trying local cache", appKey, code, ex);
            return policyCache.get(appKey, code)
                    .orElseThrow(() -> new ZestLlmException(LlmErrorCode.ADAPTER_UNAVAILABLE, null,
                            "Control Plane unavailable and no cached policy for " + appKey + ":" + code));
        }
    }

    private PrepareResponse fetchPrepareFromControlPlane(InvokeRequest request) {
        PrepareRequest prepareRequest = toPrepareRequest(request);
        log.debug("Agent prepare code={} appKey={}", request.getCode(), request.getAppKey());
        return controlPlaneClient.post()
                .uri(PREPARE_PATH)
                .body(prepareRequest)
                .retrieve()
                .body(PrepareResponse.class);
    }

    public InvokeResponse execute(PrepareResponse prepared) {
        return execute(prepared, null);
    }

    public InvokeResponse execute(PrepareResponse prepared, Map<String, Object> inputs) {
        return execute(prepared, inputs, false, null);
    }

    /**
     * 流式执行（直连 LiteLLM SSE，不经 CP 转发 token）。
     */
    public InvokeResponse executeStream(PrepareResponse prepared, Map<String, Object> inputs,
                                        Consumer<StreamChunk> chunkConsumer) {
        return execute(prepared, inputs, true, chunkConsumer);
    }

    private InvokeResponse execute(PrepareResponse prepared, Map<String, Object> inputs,
                                   boolean streaming, Consumer<StreamChunk> chunkConsumer) {
        long start = System.currentTimeMillis();
        List<String> models = new ArrayList<>();
        if (prepared.getModel() != null) {
            models.add(prepared.getModel());
        }
        if (prepared.getFallbackModels() != null) {
            models.addAll(prepared.getFallbackModels());
        }

        RestClientException lastError = null;
        for (String model : models) {
            if (model == null || model.isBlank()) {
                continue;
            }
            try {
                if (streaming) {
                    return doExecuteStream(prepared, model, start, inputs, chunkConsumer);
                }
                return doExecute(prepared, model, start, inputs);
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("LiteLLM model failed model={} traceId={}", model, prepared.getTraceId(), ex);
            }
        }
        InvokeResponse failed = new InvokeResponse();
        failed.setTraceId(prepared.getTraceId());
        failed.setCode(prepared.getCode());
        failed.setPromptVersion(prepared.getPromptVersion());
        failed.setStatus("FAILED");
        failed.setErrorCode("MODEL_TIMEOUT");
        failed.setErrorMessage(lastError != null ? lastError.getMessage() : "No model configured");
        InvokeMetrics metrics = new InvokeMetrics();
        metrics.setLatencyMs(System.currentTimeMillis() - start);
        failed.setMetrics(metrics);
        return failed;
    }

    public void report(ReportRequest reportRequest) {
        reportExecutor.submit(() -> {
            try {
                log.debug("Agent report traceId={} status={}", reportRequest.getTraceId(), reportRequest.getStatus());
                controlPlaneClient.post()
                        .uri(REPORT_PATH)
                        .body(reportRequest)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception ex) {
                log.warn("Agent report failed traceId={}", reportRequest.getTraceId(), ex);
            }
        });
    }

    public ReportRequest buildReport(InvokeRequest request, PrepareResponse prepared, InvokeResponse response) {
        ReportRequest report = new ReportRequest();
        report.setTraceId(prepared.getTraceId());
        report.setAppKey(request.getAppKey());
        report.setCode(prepared.getCode());
        report.setBizId(request.getBizId());
        report.setStatus(response.getStatus());
        report.setModel(response.getModel());
        report.setPromptVersion(prepared.getPromptVersion());
        report.setOutput(response.getOutput());
        report.setErrorCode(response.getErrorCode());
        report.setErrorMessage(response.getErrorMessage());
        if (response.getMetrics() != null) {
            report.setLatencyMs(response.getMetrics().getLatencyMs());
            report.setPromptTokens(response.getMetrics().getPromptTokens());
            report.setCompletionTokens(response.getMetrics().getCompletionTokens());
            report.setCost(response.getMetrics().getCost());
        }
        return report;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private InvokeResponse doExecute(PrepareResponse prepared, String model, long start, Map<String, Object> inputs) {
        RestClient client = resolveLitellmClient(prepared);
        String basePrompt = GuardrailsEnforcer.enforcePrompt(
                prepared.getRenderedPrompt(), prepared.getGuardrails(), prepared.getTraceId(), contentModerationAdapter);

        String content;
        JsonNode usage;
        if (useToolLoop(prepared)) {
            ToolLoopParams params = ToolLoopParams.builder()
                    .traceId(prepared.getTraceId())
                    .tools(prepared.getTools())
                    .maxToolSteps(prepared.getMaxToolSteps())
                    .maxTokens(prepared.getMaxTokens())
                    .temperature(prepared.getTemperature())
                    .build();
            FunctionCallLoop.LoopResult loopResult =
                    functionCallLoop.run(client, params, model, basePrompt, inputs);
            content = loopResult.content();
            usage = loopResult.usage();
        } else {
            String prompt = toolOrchestrator.enrichPrompt(prepared.getRenderedPrompt(), prepared.getTools(), inputs, prepared.getTraceId());
            GuardrailsEnforcer.checkPromptLength(prompt, prepared.getGuardrails(), prepared.getTraceId());
            prompt = GuardrailsEnforcer.sanitizePrompt(prompt, prepared.getGuardrails());
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            if (prepared.getMaxTokens() != null) {
                body.put("max_tokens", prepared.getMaxTokens());
            }
            if (prepared.getTemperature() != null) {
                body.put("temperature", prepared.getTemperature());
            }
            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(objectMapper.createObjectNode().put("role", "user").put("content", prompt));
            body.set("messages", messages);

            String raw = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            try {
                JsonNode root = objectMapper.readTree(raw);
                content = root.path("choices").path(0).path("message").path("content").asText("");
                usage = root.path("usage");
            } catch (JsonProcessingException ex) {
                throw new RestClientException("Invalid LiteLLM response", ex);
            }
        }

        try {
            Map<String, Object> output = parseOutput(content);
            output = GuardrailsEnforcer.enforceOutput(
                    output, prepared.getGuardrails(), prepared.getTraceId(), contentModerationAdapter);
            if (GuardrailsEnforcer.shouldValidateSchema(prepared.getGuardrails())) {
                ValidationResult validation = outputSchemaValidator.validate(prepared.getOutputSchema(), output);
                if (!validation.isValid()) {
                    throw new ZestLlmException(LlmErrorCode.OUTPUT_SCHEMA_MISMATCH, prepared.getTraceId(),
                            validation.getMessage());
                }
            }

            InvokeMetrics metrics = new InvokeMetrics();
            metrics.setLatencyMs(System.currentTimeMillis() - start);
            if (usage != null && !usage.isMissingNode()) {
                metrics.setPromptTokens(usage.path("prompt_tokens").asInt(0));
                metrics.setCompletionTokens(usage.path("completion_tokens").asInt(0));
            }

            InvokeResponse response = new InvokeResponse();
            response.setTraceId(prepared.getTraceId());
            response.setCode(prepared.getCode());
            response.setPromptVersion(prepared.getPromptVersion());
            response.setStatus("SUCCESS");
            response.setModel(model);
            response.setOutput(output);
            response.setMetrics(metrics);
            return response;
        } catch (JsonProcessingException ex) {
            throw new RestClientException("Invalid output", ex);
        }
    }

    private InvokeResponse doExecuteStream(PrepareResponse prepared, String model, long start,
                                           Map<String, Object> inputs, Consumer<StreamChunk> chunkConsumer) {
        GatewayEndpoint endpoint = resolveGatewayEndpoint(prepared);
        String prompt = useToolLoop(prepared)
                ? GuardrailsEnforcer.sanitizePrompt(prepared.getRenderedPrompt(), prepared.getGuardrails())
                : toolOrchestrator.enrichPrompt(prepared.getRenderedPrompt(), prepared.getTools(), inputs, prepared.getTraceId());
        GuardrailsEnforcer.checkPromptLength(prompt, prepared.getGuardrails(), prepared.getTraceId());

        StringBuilder contentBuilder = new StringBuilder();
        if (useToolLoop(prepared)) {
            RestClient client = resolveLitellmClient(prepared);
            ToolLoopParams params = ToolLoopParams.builder()
                    .traceId(prepared.getTraceId())
                    .tools(prepared.getTools())
                    .maxToolSteps(prepared.getMaxToolSteps())
                    .maxTokens(prepared.getMaxTokens())
                    .temperature(prepared.getTemperature())
                    .build();
            FunctionCallLoop.LoopResult loopResult =
                    functionCallLoop.run(client, params, model, prompt, inputs);
            streamHandler.emitTextAsStream(loopResult.content(), delta -> {
                contentBuilder.append(delta);
                if (chunkConsumer != null) {
                    chunkConsumer.accept(StreamChunk.builder()
                            .traceId(prepared.getTraceId())
                            .type("delta")
                            .delta(delta)
                            .done(false)
                            .build());
                }
            }, () -> {
                if (chunkConsumer != null) {
                    chunkConsumer.accept(StreamChunk.builder()
                            .traceId(prepared.getTraceId())
                            .type("done")
                            .done(true)
                            .build());
                }
            });
        } else {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("stream", true);
            if (prepared.getMaxTokens() != null) {
                body.put("max_tokens", prepared.getMaxTokens());
            }
            if (prepared.getTemperature() != null) {
                body.put("temperature", prepared.getTemperature());
            }
            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(objectMapper.createObjectNode().put("role", "user").put("content", prompt));
            body.set("messages", messages);

            streamHandler.streamPost(endpoint.baseUrl(), endpoint.apiKey(), body.toString(),
                    delta -> {
                        contentBuilder.append(delta);
                        if (chunkConsumer != null) {
                            chunkConsumer.accept(StreamChunk.builder()
                                    .traceId(prepared.getTraceId())
                                    .type("delta")
                                    .delta(delta)
                                    .done(false)
                                    .build());
                        }
                    },
                    () -> {
                        if (chunkConsumer != null) {
                            chunkConsumer.accept(StreamChunk.builder()
                                    .traceId(prepared.getTraceId())
                                    .type("done")
                                    .done(true)
                                    .build());
                        }
                    });
        }

        try {
            Map<String, Object> output = parseOutput(contentBuilder.toString());
            output = GuardrailsEnforcer.enforceOutput(
                    output, prepared.getGuardrails(), prepared.getTraceId(), contentModerationAdapter);
            if (GuardrailsEnforcer.shouldValidateSchema(prepared.getGuardrails())) {
                ValidationResult validation = outputSchemaValidator.validate(prepared.getOutputSchema(), output);
                if (!validation.isValid()) {
                    throw new ZestLlmException(LlmErrorCode.OUTPUT_SCHEMA_MISMATCH, prepared.getTraceId(),
                            validation.getMessage());
                }
            }
            InvokeMetrics metrics = new InvokeMetrics();
            metrics.setLatencyMs(System.currentTimeMillis() - start);
            InvokeResponse response = new InvokeResponse();
            response.setTraceId(prepared.getTraceId());
            response.setCode(prepared.getCode());
            response.setPromptVersion(prepared.getPromptVersion());
            response.setStatus("SUCCESS");
            response.setModel(model);
            response.setOutput(output);
            response.setMetrics(metrics);
            return response;
        } catch (JsonProcessingException ex) {
            throw new RestClientException("Invalid stream output", ex);
        }
    }

    private boolean useToolLoop(PrepareResponse prepared) {
        if (prepared.getTools() == null || prepared.getTools().isEmpty()) {
            return false;
        }
        return !"prefetch".equalsIgnoreCase(prepared.getToolCallMode());
    }

    private GatewayEndpoint resolveGatewayEndpoint(PrepareResponse prepared) {
        String baseUrl = prepared.getGatewayBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = properties.getLitellmUrl();
        }
        String apiKey = toolOrchestrator.resolveGatewayApiKey(prepared.getOutboundSecretRef(), properties.getLitellmApiKey());
        return new GatewayEndpoint(baseUrl, apiKey);
    }

    private record GatewayEndpoint(String baseUrl, String apiKey) {
    }

    private RestClient resolveLitellmClient(PrepareResponse prepared) {
        String baseUrl = prepared.getGatewayBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return litellmClient;
        }
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        String apiKey = toolOrchestrator.resolveGatewayApiKey(prepared.getOutboundSecretRef(), properties.getLitellmApiKey());
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }

    private Map<String, Object> parseOutput(String content) throws JsonProcessingException {
        if (content == null || content.isBlank()) {
            return Map.of("answer", "");
        }
        try {
            return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("answer", content);
            return fallback;
        }
    }

    private PrepareRequest toPrepareRequest(InvokeRequest request) {
        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setAppKey(request.getAppKey());
        prepareRequest.setCode(request.getCode());
        prepareRequest.setBizId(request.getBizId());
        prepareRequest.setInputs(request.getInputs());
        prepareRequest.setContext(request.getContext());
        prepareRequest.setOptions(request.getOptions());
        return prepareRequest;
    }

    private void fillDefaults(InvokeRequest request) {
        if (request.getAppKey() == null) {
            request.setAppKey(properties.getAppKey());
        }
    }
}
