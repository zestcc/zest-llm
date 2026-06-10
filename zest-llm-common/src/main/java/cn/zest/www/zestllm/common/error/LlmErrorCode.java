package cn.zest.www.zestllm.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LlmErrorCode {
    AUTH_FAILED("AUTH_FAILED", "鉴权失败"),
    QUOTA_EXCEEDED("QUOTA_EXCEEDED", "配额超限"),
    PROMPT_NOT_FOUND("PROMPT_NOT_FOUND", "无已发布 Prompt"),
    TASK_NOT_FOUND("TASK_NOT_FOUND", "AI 作业不存在"),
    MODEL_TIMEOUT("MODEL_TIMEOUT", "模型调用超时或失败"),
    OUTPUT_SCHEMA_MISMATCH("OUTPUT_SCHEMA_MISMATCH", "输出不符合 schema"),
    POLICY_VIOLATION("POLICY_VIOLATION", "策略校验失败"),
    ADAPTER_UNAVAILABLE("ADAPTER_UNAVAILABLE", "适配器不可用"),
    INTERNAL_ERROR("INTERNAL_ERROR", "内部错误");

    private final String code;
    private final String message;
}
