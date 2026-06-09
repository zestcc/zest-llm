package cn.zest.www.zestllm.admin.exception;

import cn.zest.www.zestllm.admin.controller.registry.MethodRegistryController;
import cn.zest.www.zestllm.admin.controller.runtime.RuntimeLlmController;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.api.PrepareResponse;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import com.zestflow.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

/**
 * Runtime API（/v1/**）专用异常处理：返回 Starter 可解析的业务错误契约，而非 Admin Result 包装。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {RuntimeLlmController.class, MethodRegistryController.class})
public class RuntimeExceptionHandler {

    @ExceptionHandler(ZestLlmException.class)
    public ResponseEntity<?> handleZestLlmException(ZestLlmException ex, HandlerMethod handlerMethod) {
        log.warn("Runtime ZestLlmException: code={} traceId={} msg={}",
                ex.getErrorCode().getCode(), ex.getTraceId(), ex.getMessage());

        if (handlerMethod != null && handlerMethod.getBeanType() == MethodRegistryController.class) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.fail(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), ex.getErrorCode().getCode()));
        }

        Class<?> returnType = handlerMethod != null ? handlerMethod.getMethod().getReturnType() : InvokeResponse.class;
        if (returnType == PrepareResponse.class) {
            return ResponseEntity.ok(toFailedPrepare(ex));
        }
        if (returnType == void.class || returnType == Void.class) {
            return ResponseEntity.status(mapHttpStatus(ex.getErrorCode()))
                    .body(Map.of(
                            "errorCode", ex.getErrorCode().getCode(),
                            "errorMessage", ex.getMessage(),
                            "traceId", ex.getTraceId() != null ? ex.getTraceId() : ""
                    ));
        }
        return ResponseEntity.ok(toFailedInvoke(ex));
    }

    private InvokeResponse toFailedInvoke(ZestLlmException ex) {
        InvokeResponse response = new InvokeResponse();
        response.setTraceId(ex.getTraceId());
        response.setStatus("FAILED");
        response.setErrorCode(ex.getErrorCode().getCode());
        response.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : ex.getErrorCode().getMessage());
        return response;
    }

    private PrepareResponse toFailedPrepare(ZestLlmException ex) {
        PrepareResponse response = new PrepareResponse();
        response.setTraceId(ex.getTraceId());
        return response;
    }

    private HttpStatus mapHttpStatus(LlmErrorCode code) {
        if (code == LlmErrorCode.AUTH_FAILED) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == LlmErrorCode.QUOTA_EXCEEDED) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
