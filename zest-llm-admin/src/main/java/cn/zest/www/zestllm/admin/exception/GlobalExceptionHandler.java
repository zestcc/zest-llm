package cn.zest.www.zestllm.admin.exception;

import cn.zest.www.zestllm.common.error.ZestLlmException;
import com.zestflow.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ZestLlmException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleZestLlmException(ZestLlmException ex) {
        log.warn("ZestLlmException: code={} traceId={} msg={}",
                ex.getErrorCode().getCode(), ex.getTraceId(), ex.getMessage());
        return Result.fail(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), ex.getErrorCode().getCode());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException: code={} msg={}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.getHttpCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(Result.fail(ex.getHttpCode(), ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(Exception ex) {
        return Result.fail(HttpStatus.BAD_REQUEST.value(), "参数校验失败");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(Exception ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return Result.fail(HttpStatus.NOT_FOUND.value(), "资源不存在", "NOT_FOUND");
    }

    /** SSE 等已提交响应的场景，避免 ErrorMvc 二次渲染报错 */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncNotUsable(AsyncRequestNotUsableException ex) {
        log.debug("Async response no longer usable: {}", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return Result.fail(HttpStatus.FORBIDDEN.value(), "无访问权限", "ACCESS_DENIED");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "内部错误", "INTERNAL_ERROR");
    }
}
