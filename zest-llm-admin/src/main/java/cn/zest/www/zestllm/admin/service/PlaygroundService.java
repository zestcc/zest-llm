package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.dto.PlaygroundPreviewCommand;
import cn.zest.www.zestllm.admin.model.dto.PlaygroundRunCommand;
import cn.zest.www.zestllm.admin.model.dto.ResolvedPolicy;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.vo.PlaygroundPreviewVO;
import cn.zest.www.zestllm.admin.model.vo.PlaygroundRunVO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.infra.guardrails.GuardrailsEnforcer;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class PlaygroundService {

    private final RuntimePolicyService runtimePolicyService;
    private final LlmInvokeService llmInvokeService;
    private final LlmAppRepo appRepo;
    private final ContentModerationAdapter contentModerationAdapter;

    public PlaygroundPreviewVO preview(PlaygroundPreviewCommand command) {
        LlmAppDO app = loadApp(command.getAppKey());
        String traceId = TokenHashUtil.newTraceId();
        ResolvedPolicy resolved = runtimePolicyService.resolvePolicy(
                app, command.getCode(), command.getInputs(), traceId);
        CachedPolicy policy = resolved.getPolicy();
        GuardrailsConfig guardrails = policy.getGuardrails();
        String renderedPrompt = GuardrailsEnforcer.enforcePrompt(
                resolved.getRenderedPrompt(), guardrails, traceId, contentModerationAdapter);

        return PlaygroundPreviewVO.builder()
                .traceId(traceId)
                .code(resolved.getTask().getCode())
                .promptVersion(policy.getPromptVersion())
                .renderedPrompt(renderedPrompt)
                .model(policy.getPrimaryModel())
                .fallbackModels(policy.getFallbackModels())
                .maxTokens(policy.getMaxTokens())
                .temperature(policy.getTemperature())
                .outputSchema(policy.getOutputSchema())
                .guardrails(guardrails)
                .build();
    }

    public SseEmitter runStream(PlaygroundRunCommand command) {
        InvokeRequest request = new InvokeRequest();
        request.setAppKey(command.getAppKey());
        request.setCode(command.getCode());
        request.setInputs(command.getInputs());
        request.setBizId(command.getBizId());
        return llmInvokeService.invokeStreamForAdmin(request);
    }

    public PlaygroundRunVO run(PlaygroundRunCommand command) {
        InvokeRequest request = new InvokeRequest();
        request.setAppKey(command.getAppKey());
        request.setCode(command.getCode());
        request.setInputs(command.getInputs());
        request.setBizId(command.getBizId());

        try {
            InvokeResponse response = llmInvokeService.invokeForAdmin(request);
            return PlaygroundRunVO.builder()
                    .traceId(response.getTraceId())
                    .status(response.getStatus())
                    .code(response.getCode())
                    .promptVersion(response.getPromptVersion())
                    .model(response.getModel())
                    .output(response.getOutput())
                    .metrics(response.getMetrics())
                    .cacheHit(response.getMetrics() != null ? response.getMetrics().getCacheHit() : null)
                    .build();
        } catch (ZestLlmException ex) {
            return PlaygroundRunVO.builder()
                    .traceId(ex.getTraceId())
                    .status("FAILED")
                    .code(command.getCode())
                    .errorCode(ex.getErrorCode() != null ? ex.getErrorCode().name() : null)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    private LlmAppDO loadApp(String appKey) {
        return appRepo.findByAppKey(appKey)
                .filter(app -> "ACTIVE".equals(app.getStatus()))
                .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在或未激活: " + appKey));
    }
}
