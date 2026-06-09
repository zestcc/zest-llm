package cn.zest.www.zestllm.common.error;

import lombok.Getter;

@Getter
public class ZestLlmException extends RuntimeException {

    private final LlmErrorCode errorCode;
    private final String traceId;

    public ZestLlmException(LlmErrorCode errorCode) {
        this(errorCode, null, errorCode.getMessage());
    }

    public ZestLlmException(LlmErrorCode errorCode, String traceId) {
        this(errorCode, traceId, errorCode.getMessage());
    }

    public ZestLlmException(LlmErrorCode errorCode, String traceId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.traceId = traceId;
    }
}
