package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.dto.ResolvedPolicy;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.api.PrepareRequest;
import cn.zest.www.zestllm.common.api.PrepareResponse;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmPrepareService {

    private static final int DEFAULT_ESTIMATED_TOKENS = 512;

    private final RuntimePolicyService runtimePolicyService;
    private final QuotaAdapter quotaAdapter;
    private final LlmExecutionRepo executionRepo;
    private final ObjectMapper objectMapper;
    private final ObservabilityAdapter observabilityAdapter;

    @Transactional(rollbackFor = Exception.class)
    public PrepareResponse prepare(String bearerToken, PrepareRequest request) {
        String traceId = TokenHashUtil.newTraceId();
        LlmAppDO app = runtimePolicyService.authenticate(request.getAppKey(), bearerToken);
        quotaAdapter.checkAndConsume(app.getId(), estimateTokens(request));

        observabilityAdapter.traceStart(cn.zest.www.zestllm.spi.model.TraceStartEvent.builder()
                .traceId(traceId)
                .appKey(app.getAppKey())
                .code(request.getCode())
                .build());

        ResolvedPolicy resolved = runtimePolicyService.resolvePolicy(
                app, request.getCode(), request.getInputs(), traceId);
        CachedPolicy policy = resolved.getPolicy();

        savePendingExecution(traceId, app, resolved, request);

        PrepareResponse response = new PrepareResponse();
        response.setTraceId(traceId);
        response.setCode(resolved.getTask().getCode());
        response.setPromptVersion(policy.getPromptVersion());
        response.setRenderedPrompt(resolved.getRenderedPrompt());
        response.setModel(policy.getPrimaryModel());
        response.setFallbackModels(policy.getFallbackModels());
        response.setMaxTokens(policy.getMaxTokens());
        response.setTemperature(policy.getTemperature());
        response.setTimeoutMs(resolveTimeout(request, policy));
        response.setOutputSchema(policy.getOutputSchema());
        return response;
    }

    private int estimateTokens(PrepareRequest request) {
        if (request.getOptions() != null && request.getOptions().getTimeoutMs() != null) {
            return DEFAULT_ESTIMATED_TOKENS;
        }
        return DEFAULT_ESTIMATED_TOKENS;
    }

    private Integer resolveTimeout(PrepareRequest request, CachedPolicy policy) {
        if (request.getOptions() != null && request.getOptions().getTimeoutMs() != null) {
            return request.getOptions().getTimeoutMs().intValue();
        }
        return policy.getTimeoutMs();
    }

    private void savePendingExecution(String traceId, LlmAppDO app, ResolvedPolicy resolved, PrepareRequest request) {
        LlmExecutionDO execution = new LlmExecutionDO();
        execution.setTraceId(traceId);
        execution.setAppId(app.getId());
        execution.setTaskId(resolved.getTask().getId());
        execution.setTaskCode(resolved.getTask().getCode());
        execution.setBizId(request.getBizId());
        execution.setPromptVersion(resolved.getPolicy().getPromptVersion());
        execution.setModel(resolved.getPolicy().getPrimaryModel());
        execution.setStatus("PENDING");
        execution.setInputJson(toJson(request.getInputs()));
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
            log.warn("Failed to serialize inputs traceId={}", ex.getMessage());
            return null;
        }
    }
}
