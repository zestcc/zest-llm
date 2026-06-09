package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.dto.InvokeCommand;
import cn.zest.www.zestllm.admin.model.dto.ResolvedPolicy;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.api.InvokeMetrics;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.model.ChatRequest;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.TraceEndEvent;
import cn.zest.www.zestllm.spi.model.TraceStartEvent;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;
import cn.zest.www.zestllm.spi.schema.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmInvokeService {

    private static final int DEFAULT_ESTIMATED_TOKENS = 512;

    private final RuntimePolicyService runtimePolicyService;
    private final LlmExecutionRepo executionRepo;
    private final ModelGatewayAdapter modelGatewayAdapter;
    private final ObservabilityAdapter observabilityAdapter;
    private final OutputSchemaValidator outputSchemaValidator;
    private final QuotaAdapter quotaAdapter;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public InvokeResponse invoke(String bearerToken, InvokeRequest request) {
        InvokeCommand command = new InvokeCommand();
        command.setBearerToken(bearerToken);
        command.setRequest(request);
        return invoke(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public InvokeResponse invoke(InvokeCommand command) {
        InvokeRequest request = command.getRequest();
        String traceId = TokenHashUtil.newTraceId();
        long start = System.currentTimeMillis();

        LlmAppDO app = runtimePolicyService.authenticate(request.getAppKey(), command.getBearerToken());
        quotaAdapter.checkAndConsume(app.getId(), DEFAULT_ESTIMATED_TOKENS);

        ResolvedPolicy resolved = runtimePolicyService.resolvePolicy(
                app, request.getCode(), request.getInputs(), traceId);
        CachedPolicy policy = resolved.getPolicy();

        observabilityAdapter.traceStart(TraceStartEvent.builder()
                .traceId(traceId)
                .appKey(app.getAppKey())
                .code(resolved.getTask().getCode())
                .model(policy.getPrimaryModel())
                .metadata(request.getContext())
                .build());

        ChatRequest chatRequest = ChatRequest.builder()
                .traceId(traceId)
                .model(policy.getPrimaryModel())
                .userMessage(resolved.getRenderedPrompt())
                .maxTokens(policy.getMaxTokens())
                .temperature(policy.getTemperature())
                .fallbackModels(policy.getFallbackModels())
                .build();

        InvokeResponse response = new InvokeResponse();
        response.setTraceId(traceId);
        response.setCode(resolved.getTask().getCode());
        response.setPromptVersion(policy.getPromptVersion());

        try {
            ChatResponse chatResponse = modelGatewayAdapter.chat(chatRequest);
            Map<String, Object> output = parseOutput(chatResponse.getContent());
            ValidationResult validation = outputSchemaValidator.validate(policy.getOutputSchema(), output);
            if (!validation.isValid()) {
                throw new ZestLlmException(LlmErrorCode.OUTPUT_SCHEMA_MISMATCH, traceId,
                        validation.getMessage());
            }

            InvokeMetrics metrics = buildMetrics(chatResponse, start);
            response.setStatus("SUCCESS");
            response.setModel(chatResponse.getModel());
            response.setOutput(output);
            response.setMetrics(metrics);

            saveExecution(traceId, app, resolved, request, chatResponse, "SUCCESS", output, null, null, metrics);
            observabilityAdapter.traceEnd(buildTraceEnd(traceId, true, null, metrics));
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
