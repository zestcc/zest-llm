package cn.zest.www.zestllm.admin.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final int httpCode;

    public BusinessException(String errorCode, String message) {
        this(errorCode, message, 400);
    }

    public BusinessException(String errorCode, String message, int httpCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpCode = httpCode;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException("BAD_REQUEST", message, 400);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException("UNAUTHORIZED", message, 401);
    }
}
