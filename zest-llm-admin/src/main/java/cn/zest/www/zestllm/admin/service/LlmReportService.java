package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.common.api.ReportRequest;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.model.TraceEndEvent;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.report.ReportChannelAdapter;
import cn.zest.www.zestllm.spi.report.ReportDelivery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmReportService {

    private final AppAuthService appAuthService;
    private final LlmExecutionRepo executionRepo;
    private final ObservabilityAdapter observabilityAdapter;
    private final ReportChannelAdapter reportChannelAdapter;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void report(String bearerToken, ReportRequest request) {
        appAuthService.authenticate(request.getAppKey(), bearerToken);

        LlmExecutionDO execution = executionRepo.findByTraceId(request.getTraceId())
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.INTERNAL_ERROR, request.getTraceId(), "Execution 不存在"));

        execution.setStatus(request.getStatus());
        execution.setModel(request.getModel());
        execution.setPromptVersion(request.getPromptVersion());
        execution.setOutputJson(toJson(request.getOutput()));
        execution.setErrorCode(request.getErrorCode());
        execution.setErrorMessage(request.getErrorMessage());
        execution.setLatencyMs(request.getLatencyMs());
        execution.setPromptTokens(request.getPromptTokens());
        execution.setCompletionTokens(request.getCompletionTokens());
        execution.setCost(request.getCost() != null ? BigDecimal.valueOf(request.getCost()) : null);
        executionRepo.updateByTraceId(execution);

        observabilityAdapter.traceEnd(TraceEndEvent.builder()
                .traceId(request.getTraceId())
                .success("SUCCESS".equals(request.getStatus()))
                .errorCode(request.getErrorCode())
                .promptTokens(request.getPromptTokens())
                .completionTokens(request.getCompletionTokens())
                .cost(request.getCost())
                .latencyMs(request.getLatencyMs())
                .build());

        reportChannelAdapter.deliver(ReportDelivery.builder()
                .traceId(request.getTraceId())
                .appKey(request.getAppKey())
                .code(execution.getTaskCode())
                .status(request.getStatus())
                .model(request.getModel())
                .promptVersion(request.getPromptVersion())
                .output(request.getOutput())
                .errorCode(request.getErrorCode())
                .errorMessage(request.getErrorMessage())
                .latencyMs(request.getLatencyMs())
                .promptTokens(request.getPromptTokens())
                .completionTokens(request.getCompletionTokens())
                .cost(request.getCost())
                .build());
    }

    private String toJson(Map<String, Object> output) {
        if (output == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException ex) {
            throw new ZestLlmException(LlmErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
