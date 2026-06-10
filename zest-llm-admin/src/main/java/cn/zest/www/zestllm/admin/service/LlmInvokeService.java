package cn.zest.www.zestllm.admin.service;



import cn.zest.www.zestllm.admin.model.dto.InvokeCommand;

import cn.zest.www.zestllm.admin.model.dto.ResolvedPolicy;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;

import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;

import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;

import cn.zest.www.zestllm.admin.util.TokenHashUtil;

import cn.zest.www.zestllm.common.api.InvokeMetrics;

import cn.zest.www.zestllm.common.api.InvokeRequest;

import cn.zest.www.zestllm.common.api.InvokeResponse;

import cn.zest.www.zestllm.common.error.LlmErrorCode;

import cn.zest.www.zestllm.common.error.ZestLlmException;

import cn.zest.www.zestllm.infra.cache.ValkeyResponseCacheAdapter;

import cn.zest.www.zestllm.infra.gateway.SseStreamHandler;

import cn.zest.www.zestllm.infra.guardrails.GuardrailsEnforcer;

import cn.zest.www.zestllm.infra.tool.FunctionCallLoop;

import cn.zest.www.zestllm.infra.tool.ToolLoopParams;

import cn.zest.www.zestllm.infra.tool.ToolOrchestrator;

import cn.zest.www.zestllm.infra.config.LiteLLMProperties;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.cache.CachedResponse;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;

import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;

import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;

import cn.zest.www.zestllm.spi.model.ChatRequest;

import cn.zest.www.zestllm.spi.model.ChatResponse;

import cn.zest.www.zestllm.spi.model.TraceEndEvent;

import cn.zest.www.zestllm.spi.model.TraceStartEvent;

import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;

import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;

import cn.zest.www.zestllm.spi.quota.QuotaAdapter;

import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;

import cn.zest.www.zestllm.spi.schema.ValidationResult;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestClient;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;



import java.math.BigDecimal;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;
import java.util.Optional;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;

import java.util.function.Consumer;



@Slf4j

@Service

@RequiredArgsConstructor

public class LlmInvokeService {



    private static final int DEFAULT_ESTIMATED_TOKENS = 512;



    private final RuntimePolicyService runtimePolicyService;

    private final LlmAppRepo appRepo;

    private final LlmExecutionRepo executionRepo;

    private final ModelGatewayAdapter modelGatewayAdapter;

    private final ObservabilityAdapter observabilityAdapter;

    private final OutputSchemaValidator outputSchemaValidator;

    private final QuotaAdapter quotaAdapter;

    private final ObjectMapper objectMapper;

    private final ToolOrchestrator toolOrchestrator;

    private final FunctionCallLoop functionCallLoop;

    private final SseStreamHandler sseStreamHandler;

    private final RestClient liteLlmRestClient;

    private final LiteLLMProperties liteLLMProperties;

    private final ContentModerationAdapter contentModerationAdapter;

    private final ResponseCacheAdapter responseCacheAdapter;

    private final CostAlertService costAlertService;



    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {

        Thread t = new Thread(r, "cp-llm-stream");

        t.setDaemon(true);

        return t;

    });



    @Transactional(rollbackFor = Exception.class)

    public InvokeResponse invoke(String bearerToken, InvokeRequest request) {

        InvokeCommand command = new InvokeCommand();

        command.setBearerToken(bearerToken);

        command.setRequest(request);

        return invoke(command);

    }



    @Transactional(rollbackFor = Exception.class)

    public InvokeResponse invokeForAdmin(InvokeRequest request) {

        InvokeCommand command = new InvokeCommand();

        command.setRequest(request);

        command.setAdminBypass(true);

        return invoke(command);

    }



    @Transactional(rollbackFor = Exception.class)

    public InvokeResponse invoke(InvokeCommand command) {

        InvokeRequest request = command.getRequest();

        String traceId = TokenHashUtil.newTraceId();

        long start = System.currentTimeMillis();



        LlmAppDO app = command.isAdminBypass()

                ? appRepo.findByAppKey(request.getAppKey())

                    .filter(a -> "ACTIVE".equals(a.getStatus()))

                    .orElseThrow(() -> new ZestLlmException(LlmErrorCode.AUTH_FAILED))

                : runtimePolicyService.authenticate(request.getAppKey(), command.getBearerToken());

        quotaAdapter.checkAndConsume(app.getId(), DEFAULT_ESTIMATED_TOKENS);



        ResolvedPolicy resolved = runtimePolicyService.resolvePolicy(

                app, request.getCode(), request.getInputs(), traceId);

        CachedPolicy policy = resolved.getPolicy();

        GuardrailsConfig guardrails = policy.getGuardrails();

        String renderedPrompt = GuardrailsEnforcer.enforcePrompt(
                resolved.getRenderedPrompt(), guardrails, traceId, contentModerationAdapter);



        if (!ToolOrchestrator.shouldUseToolLoop(policy.getTools(), policy.getToolCallMode())) {

            renderedPrompt = toolOrchestrator.enrichPrompt(

                    renderedPrompt, policy.getTools(), request.getInputs(), traceId);

            GuardrailsEnforcer.checkPromptLength(renderedPrompt, guardrails, traceId);

        }



        observabilityAdapter.traceStart(TraceStartEvent.builder()

                .traceId(traceId)

                .appKey(app.getAppKey())

                .code(resolved.getTask().getCode())

                .model(policy.getPrimaryModel())

                .metadata(request.getContext())

                .build());



        InvokeResponse response = new InvokeResponse();

        response.setTraceId(traceId);

        response.setCode(resolved.getTask().getCode());

        response.setPromptVersion(policy.getPromptVersion());



        boolean useToolLoop = ToolOrchestrator.shouldUseToolLoop(policy.getTools(), policy.getToolCallMode());

        String responseCacheKey = null;

        if (!useToolLoop) {

            String promptHash = ValkeyResponseCacheAdapter.hashPrompt(renderedPrompt);

            responseCacheKey = ResponseCacheAdapter.buildKey(

                    app.getAppKey(), resolved.getTask().getCode(), policy.getPrimaryModel(), promptHash);

            Optional<CachedResponse> cachedHit = responseCacheAdapter.get(responseCacheKey);

            if (cachedHit.isPresent()) {

                CachedResponse hit = cachedHit.get();

                InvokeMetrics cacheMetrics = new InvokeMetrics();

                cacheMetrics.setLatencyMs(System.currentTimeMillis() - start);

                cacheMetrics.setCacheHit(true);

                response.setStatus(hit.getStatus() != null ? hit.getStatus() : "SUCCESS");

                response.setModel(hit.getModel() != null ? hit.getModel() : policy.getPrimaryModel());

                response.setOutput(hit.getOutput());

                response.setMetrics(cacheMetrics);

                saveExecution(traceId, app, resolved, request, null, response.getStatus(),

                        hit.getOutput(), null, null, cacheMetrics);

                observabilityAdapter.traceEnd(buildTraceEnd(traceId, true, null, cacheMetrics));

                costAlertService.checkAfterInvoke(app);

                return response;

            }

        }



        try {

            ChatResponse chatResponse = executeChat(resolved, policy, renderedPrompt, request.getInputs(), traceId, start);

            Map<String, Object> output = parseOutput(chatResponse.getContent());

            output = GuardrailsEnforcer.enforceOutput(output, guardrails, traceId, contentModerationAdapter);

            if (GuardrailsEnforcer.shouldValidateSchema(guardrails)) {

                ValidationResult validation = outputSchemaValidator.validate(policy.getOutputSchema(), output);

                if (!validation.isValid()) {

                    throw new ZestLlmException(LlmErrorCode.OUTPUT_SCHEMA_MISMATCH, traceId,

                            validation.getMessage());

                }

            }



            InvokeMetrics metrics = buildMetrics(chatResponse, start);

            response.setStatus("SUCCESS");

            response.setModel(chatResponse.getModel());

            response.setOutput(output);

            response.setMetrics(metrics);



            if (responseCacheKey != null) {

                responseCacheAdapter.put(responseCacheKey, CachedResponse.builder()

                        .status("SUCCESS")

                        .model(chatResponse.getModel())

                        .promptVersion(policy.getPromptVersion())

                        .output(output)

                        .build(), null);

            }



            saveExecution(traceId, app, resolved, request, chatResponse, "SUCCESS", output, null, null, metrics);

            observabilityAdapter.traceEnd(buildTraceEnd(traceId, true, null, metrics));

            costAlertService.checkAfterInvoke(app);

        } catch (ZestLlmException ex) {

            handleFailure(traceId, app, resolved, request, response, ex, start);

            throw ex;

        } catch (Exception ex) {

            log.warn("LLM invoke failed traceId={} code={}", traceId, resolved.getTask().getCode(), ex);

            ZestLlmException zestEx = new ZestLlmException(LlmErrorCode.MODEL_TIMEOUT, traceId, ex.getMessage());

            handleFailure(traceId, app, resolved, request, response, zestEx, start);

            return response;

        }

        return response;

    }



    public SseEmitter invokeStream(String bearerToken, InvokeRequest request) {

        SseEmitter emitter = new SseEmitter(liteLLMProperties.getReadTimeoutMs());

        streamExecutor.submit(() -> {

            String traceId = TokenHashUtil.newTraceId();

            long start = System.currentTimeMillis();

            try {

                LlmAppDO app = runtimePolicyService.authenticate(request.getAppKey(), bearerToken);

                quotaAdapter.checkAndConsume(app.getId(), DEFAULT_ESTIMATED_TOKENS);

                ResolvedPolicy resolved = runtimePolicyService.resolvePolicy(

                        app, request.getCode(), request.getInputs(), traceId);

                CachedPolicy policy = resolved.getPolicy();

                GuardrailsConfig guardrails = policy.getGuardrails();

                String renderedPrompt = GuardrailsEnforcer.enforcePrompt(
                        resolved.getRenderedPrompt(), guardrails, traceId, contentModerationAdapter);

                GuardrailsEnforcer.checkPromptLength(renderedPrompt, guardrails, traceId);



                observabilityAdapter.traceStart(TraceStartEvent.builder()

                        .traceId(traceId)

                        .appKey(app.getAppKey())

                        .code(resolved.getTask().getCode())

                        .model(policy.getPrimaryModel())

                        .metadata(request.getContext())

                        .build());



                emitter.send(SseEmitter.event().name("meta").data(Map.of(

                        "traceId", traceId,

                        "code", resolved.getTask().getCode(),

                        "promptVersion", policy.getPromptVersion() != null ? policy.getPromptVersion() : ""

                )));



                StringBuilder contentBuilder = new StringBuilder();

                ChatResponse chatResponse = executeChatStream(resolved, policy, renderedPrompt,

                        request.getInputs(), traceId, start, delta -> {

                            contentBuilder.append(delta);

                            try {

                                emitter.send(SseEmitter.event().name("delta").data(delta));

                            } catch (Exception ex) {

                                throw new IllegalStateException(ex);

                            }

                        });



                Map<String, Object> output = parseOutput(contentBuilder.toString());

                output = GuardrailsEnforcer.enforceOutput(output, guardrails, traceId, contentModerationAdapter);

                if (GuardrailsEnforcer.shouldValidateSchema(guardrails)) {

                    ValidationResult validation = outputSchemaValidator.validate(policy.getOutputSchema(), output);

                    if (!validation.isValid()) {

                        throw new ZestLlmException(LlmErrorCode.OUTPUT_SCHEMA_MISMATCH, traceId,

                                validation.getMessage());

                    }

                }



                InvokeMetrics metrics = buildMetrics(chatResponse, start);

                saveExecution(traceId, app, resolved, request, chatResponse, "SUCCESS", output, null, null, metrics);

                observabilityAdapter.traceEnd(buildTraceEnd(traceId, true, null, metrics));



                emitter.send(SseEmitter.event().name("done").data(Map.of(

                        "traceId", traceId,

                        "status", "SUCCESS"

                )));

                emitter.complete();

            } catch (Exception ex) {

                log.warn("LLM stream invoke failed traceId={}", traceId, ex);

                emitter.completeWithError(ex);

            }

        });

        return emitter;

    }



    private ChatResponse executeChat(ResolvedPolicy resolved,

                                     CachedPolicy policy,

                                     String renderedPrompt,

                                     Map<String, Object> inputs,

                                     String traceId,

                                     long start) {

        if (ToolOrchestrator.shouldUseToolLoop(policy.getTools(), policy.getToolCallMode())) {

            return executeToolLoop(resolved, policy, renderedPrompt, inputs, traceId, start);

        }

        return modelGatewayAdapter.chat(ChatRequest.builder()

                .traceId(traceId)

                .model(policy.getPrimaryModel())

                .userMessage(renderedPrompt)

                .maxTokens(policy.getMaxTokens())

                .temperature(policy.getTemperature())

                .fallbackModels(policy.getFallbackModels())

                .build());

    }



    private ChatResponse executeChatStream(ResolvedPolicy resolved,

                                           CachedPolicy policy,

                                           String renderedPrompt,

                                           Map<String, Object> inputs,

                                           String traceId,

                                           long start,

                                           Consumer<String> onDelta) {

        List<String> models = buildModelList(policy);

        RestClientExceptionHolder lastError = new RestClientExceptionHolder();

        for (String model : models) {

            if (model == null || model.isBlank()) {

                continue;

            }

            try {

                if (ToolOrchestrator.shouldUseToolLoop(policy.getTools(), policy.getToolCallMode())) {

                    FunctionCallLoop.LoopResult loopResult = functionCallLoop.run(

                            resolveLitellmClient(policy),

                            buildToolLoopParams(policy, traceId),

                            model,

                            renderedPrompt,

                            inputs);

                    sseStreamHandler.emitTextAsStream(loopResult.content(), onDelta, null);

                    return ChatResponse.builder()

                            .model(model)

                            .content(loopResult.content())

                            .promptTokens(loopResult.usage() != null ? loopResult.usage().path("prompt_tokens").asInt(0) : 0)

                            .completionTokens(loopResult.usage() != null ? loopResult.usage().path("completion_tokens").asInt(0) : 0)

                            .latencyMs(System.currentTimeMillis() - start)

                            .build();

                }



                String prompt = toolOrchestrator.enrichPrompt(renderedPrompt, policy.getTools(), inputs, traceId);

                GuardrailsEnforcer.checkPromptLength(prompt, policy.getGuardrails(), traceId);

                ObjectNode body = buildStreamBody(policy, model, prompt);

                GatewayEndpoint endpoint = resolveGatewayEndpoint(policy);

                StringBuilder content = new StringBuilder();

                sseStreamHandler.streamPost(endpoint.baseUrl(), endpoint.apiKey(), body.toString(),

                        delta -> {

                            content.append(delta);

                            onDelta.accept(delta);

                        },

                        null);

                return ChatResponse.builder()

                        .model(model)

                        .content(content.toString())

                        .latencyMs(System.currentTimeMillis() - start)

                        .build();

            } catch (Exception ex) {

                lastError.value = ex;

                log.warn("LiteLLM stream model failed model={} traceId={}", model, traceId, ex);

            }

        }

        if (lastError.value instanceof RuntimeException runtimeEx) {
            throw runtimeEx;
        }
        throw new IllegalStateException(lastError.value != null ? lastError.value.getMessage() : "No model configured", lastError.value);

    }



    private ChatResponse executeToolLoop(ResolvedPolicy resolved,

                                         CachedPolicy policy,

                                         String renderedPrompt,

                                         Map<String, Object> inputs,

                                         String traceId,

                                         long start) {

        List<String> models = buildModelList(policy);

        Exception lastError = null;

        for (String model : models) {

            if (model == null || model.isBlank()) {

                continue;

            }

            try {

                FunctionCallLoop.LoopResult loopResult = functionCallLoop.run(

                        resolveLitellmClient(policy),

                        buildToolLoopParams(policy, traceId),

                        model,

                        renderedPrompt,

                        inputs);

                JsonNode usage = loopResult.usage();

                return ChatResponse.builder()

                        .model(model)

                        .content(loopResult.content())

                        .promptTokens(usage != null ? usage.path("prompt_tokens").asInt(0) : 0)

                        .completionTokens(usage != null ? usage.path("completion_tokens").asInt(0) : 0)

                        .latencyMs(System.currentTimeMillis() - start)

                        .build();

            } catch (Exception ex) {

                lastError = ex;

                log.warn("LiteLLM tool loop model failed model={} traceId={}", model, traceId, ex);

            }

        }

        if (lastError instanceof RuntimeException runtimeEx) {

            throw runtimeEx;

        }

        throw new IllegalStateException(lastError != null ? lastError.getMessage() : "No model configured", lastError);

    }



    private ToolLoopParams buildToolLoopParams(CachedPolicy policy, String traceId) {

        return ToolLoopParams.builder()

                .traceId(traceId)

                .tools(policy.getTools())

                .maxToolSteps(policy.getMaxToolSteps())

                .maxTokens(policy.getMaxTokens())

                .temperature(policy.getTemperature())

                .build();

    }



    private List<String> buildModelList(CachedPolicy policy) {

        List<String> models = new ArrayList<>();

        models.add(policy.getPrimaryModel());

        if (policy.getFallbackModels() != null) {

            models.addAll(policy.getFallbackModels());

        }

        return models;

    }



    private RestClient resolveLitellmClient(CachedPolicy policy) {

        String baseUrl = policy.getGatewayBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {

            return liteLlmRestClient;

        }

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);

        String apiKey = toolOrchestrator.resolveGatewayApiKey(

                policy.getOutboundSecretRef(), liteLLMProperties.getApiKey());

        if (apiKey != null && !apiKey.isBlank()) {

            builder.defaultHeader("Authorization", "Bearer " + apiKey);

        }

        return builder.build();

    }



    private GatewayEndpoint resolveGatewayEndpoint(CachedPolicy policy) {

        String baseUrl = policy.getGatewayBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {

            baseUrl = liteLLMProperties.getBaseUrl();

        }

        String apiKey = toolOrchestrator.resolveGatewayApiKey(

                policy.getOutboundSecretRef(), liteLLMProperties.getApiKey());

        return new GatewayEndpoint(baseUrl, apiKey);

    }



    private ObjectNode buildStreamBody(CachedPolicy policy, String model, String prompt) {

        ObjectNode body = objectMapper.createObjectNode();

        body.put("model", model);

        body.put("stream", true);

        if (policy.getMaxTokens() != null) {

            body.put("max_tokens", policy.getMaxTokens());

        }

        if (policy.getTemperature() != null) {

            body.put("temperature", policy.getTemperature());

        }

        ArrayNode messages = objectMapper.createArrayNode();

        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", prompt));

        body.set("messages", messages);

        return body;

    }



    private record GatewayEndpoint(String baseUrl, String apiKey) {

    }



    private static final class RestClientExceptionHolder {

        Exception value;

    }



    private void handleFailure(String traceId, LlmAppDO app, ResolvedPolicy resolved, InvokeRequest request,

                               InvokeResponse response, ZestLlmException ex, long start) {

        response.setStatus("FAILED");

        response.setErrorCode(ex.getErrorCode().getCode());

        response.setErrorMessage(ex.getMessage());

        InvokeMetrics metrics = buildFailedMetrics(start);

        response.setMetrics(metrics);

        saveExecution(traceId, app, resolved, request, null, "FAILED", null,

                ex.getErrorCode().getCode(), ex.getMessage(), metrics);

        observabilityAdapter.traceEnd(buildTraceEnd(traceId, false, ex.getErrorCode().getCode(), metrics));

    }



    private TraceEndEvent buildTraceEnd(String traceId, boolean success, String errorCode, InvokeMetrics metrics) {

        return TraceEndEvent.builder()

                .traceId(traceId)

                .success(success)

                .errorCode(errorCode)

                .promptTokens(metrics.getPromptTokens())

                .completionTokens(metrics.getCompletionTokens())

                .cost(metrics.getCost())

                .latencyMs(metrics.getLatencyMs())

                .build();

    }



    private Map<String, Object> parseOutput(String content) {

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



    private InvokeMetrics buildMetrics(ChatResponse chatResponse, long start) {

        InvokeMetrics metrics = new InvokeMetrics();

        metrics.setLatencyMs(chatResponse.getLatencyMs() != null

                ? chatResponse.getLatencyMs()

                : System.currentTimeMillis() - start);

        metrics.setPromptTokens(chatResponse.getPromptTokens());

        metrics.setCompletionTokens(chatResponse.getCompletionTokens());

        metrics.setCost(chatResponse.getCost());

        return metrics;

    }



    private InvokeMetrics buildFailedMetrics(long start) {

        InvokeMetrics metrics = new InvokeMetrics();

        metrics.setLatencyMs(System.currentTimeMillis() - start);

        return metrics;

    }



    private void saveExecution(String traceId, LlmAppDO app, ResolvedPolicy resolved, InvokeRequest request,

                               ChatResponse chatResponse, String status, Map<String, Object> output,

                               String errorCode, String errorMessage, InvokeMetrics metrics) {

        LlmExecutionDO execution = new LlmExecutionDO();

        execution.setTraceId(traceId);

        execution.setAppId(app.getId());

        execution.setTaskId(resolved.getTask().getId());

        execution.setTaskCode(resolved.getTask().getCode());

        execution.setBizId(request.getBizId());

        execution.setPromptVersion(resolved.getPolicy().getPromptVersion());

        execution.setModel(chatResponse != null ? chatResponse.getModel() : resolved.getPolicy().getPrimaryModel());

        execution.setStatus(status);

        execution.setInputJson(toJson(request.getInputs()));

        execution.setOutputJson(output != null ? toJson(output) : null);

        execution.setErrorCode(errorCode);

        execution.setErrorMessage(errorMessage);

        execution.setLatencyMs(metrics.getLatencyMs());

        execution.setPromptTokens(metrics.getPromptTokens());

        execution.setCompletionTokens(metrics.getCompletionTokens());

        execution.setCost(metrics.getCost() != null ? BigDecimal.valueOf(metrics.getCost()) : null);

        execution.setCreatedAt(LocalDateTime.now());

        executionRepo.insert(execution);

    }



    private String toJson(Object value) {

        if (value == null) {

            return null;

        }

        try {

            return objectMapper.writeValueAsString(value);

        } catch (JsonProcessingException ex) {

            throw new ZestLlmException(LlmErrorCode.INTERNAL_ERROR);

        }

    }

}

