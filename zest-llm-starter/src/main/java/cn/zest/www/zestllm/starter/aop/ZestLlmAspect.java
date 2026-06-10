package cn.zest.www.zestllm.starter.aop;

import cn.zest.www.zestllm.agent.LlmAgentClient;
import cn.zest.www.zestllm.common.api.InvokeOptions;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.api.PrepareResponse;
import cn.zest.www.zestllm.common.api.ReportRequest;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.starter.annotation.AiContext;
import cn.zest.www.zestllm.starter.annotation.AiInput;
import cn.zest.www.zestllm.starter.annotation.AiOutput;
import cn.zest.www.zestllm.starter.annotation.ZestLLM;
import cn.zest.www.zestllm.starter.client.LlmControlPlaneClient;
import cn.zest.www.zestllm.starter.config.ZestLlmProperties;
import cn.zest.www.zestllm.starter.mapper.AiResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class ZestLlmAspect {

    private final LlmControlPlaneClient controlPlaneClient;
    private final ObjectProvider<LlmAgentClient> agentClientProvider;
    private final AiResultMapper aiResultMapper;
    private final ZestLlmProperties properties;

    @Around("@annotation(zestLLM)")
    public Object around(ProceedingJoinPoint pjp, ZestLLM zestLLM) throws Throwable {
        InvokeRequest request = buildRequest(pjp, zestLLM);
        InvokeResponse response = isAgentMode()
                ? invokeViaAgent(request)
                : controlPlaneClient.invoke(request);

        if (response == null || !response.isSuccess()) {
            LlmErrorCode errorCode = resolveErrorCode(response);
            String traceId = response != null ? response.getTraceId() : null;
            String message = response != null ? response.getErrorMessage() : "Control Plane 无响应";
            throw new ZestLlmException(errorCode, traceId, message);
        }

        Object[] args = pjp.getArgs();
        Object outputBean = findAiOutputArg(pjp, args);
        if (outputBean != null) {
            aiResultMapper.mapToOutput(response.getOutput(), outputBean);
            applyTraceId(outputBean, response.getTraceId());
        }

        return pjp.proceed(args);
    }

    private boolean isAgentMode() {
        if (!"agent".equalsIgnoreCase(properties.getRuntimeMode())) {
            return false;
        }
        LlmAgentClient client = agentClientProvider.getIfAvailable();
        return client != null && client.isEnabled();
    }

    private InvokeResponse invokeViaAgent(InvokeRequest request) {
        LlmAgentClient agentClient = agentClientProvider.getIfAvailable();
        PrepareResponse prepared = agentClient.prepare(request);
        InvokeResponse response = agentClient.execute(prepared, request.getInputs());
        ReportRequest report = agentClient.buildReport(request, prepared, response);
        agentClient.report(report);
        return response;
    }

    private InvokeRequest buildRequest(ProceedingJoinPoint pjp, ZestLLM zestLLM) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = pjp.getArgs();

        Map<String, Object> inputs = new HashMap<>();
        Map<String, Object> context = new HashMap<>();

        for (int i = 0; i < parameters.length; i++) {
            AiInput aiInput = parameters[i].getAnnotation(AiInput.class);
            if (aiInput != null) {
                inputs.put(aiInput.value(), args[i]);
                continue;
            }
            AiContext aiContext = parameters[i].getAnnotation(AiContext.class);
            if (aiContext != null) {
                context.put(aiContext.value(), args[i]);
            }
        }

        InvokeOptions options = new InvokeOptions();
        options.setTimeoutMs(zestLLM.timeoutMs());
        options.setRetry(zestLLM.retry());

        InvokeRequest request = new InvokeRequest();
        request.setAppKey(properties.getAppKey());
        request.setCode(zestLLM.code());
        request.setInputs(inputs);
        request.setContext(context.isEmpty() ? null : context);
        request.setOptions(options);
        return request;
    }

    private Object findAiOutputArg(ProceedingJoinPoint pjp, Object[] args) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(AiOutput.class)) {
                return args[i];
            }
        }
        return null;
    }

    private LlmErrorCode resolveErrorCode(InvokeResponse response) {
        if (response == null || response.getErrorCode() == null) {
            return LlmErrorCode.INTERNAL_ERROR;
        }
        for (LlmErrorCode code : LlmErrorCode.values()) {
            if (code.getCode().equals(response.getErrorCode())) {
                return code;
            }
        }
        return LlmErrorCode.INTERNAL_ERROR;
    }

    private void applyTraceId(Object outputBean, String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return;
        }
        try {
            var field = outputBean.getClass().getDeclaredField("traceId");
            field.setAccessible(true);
            field.set(outputBean, traceId);
        } catch (NoSuchFieldException ignored) {
            // output bean 未声明 traceId 字段时跳过
        } catch (IllegalAccessException ex) {
            log.debug("Failed to set traceId on {}", outputBean.getClass().getSimpleName(), ex);
        }
    }
}
