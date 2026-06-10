package cn.zest.www.zestllm.spi.guardrails;

import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;

/**
 * 内容安全 / Moderation SPI（关键词、外部 Moderation API 等可插拔）。
 */
public interface ContentModerationAdapter {

    String adapterId();

    /**
     * 校验 Prompt 是否允许进入模型；违规应抛出 {@link cn.zest.www.zestllm.common.error.ZestLlmException}。
     */
    void checkPrompt(String prompt, GuardrailsConfig guardrails, String traceId);

    /**
     * 校验模型输出（可选）。
     */
    default void checkOutput(String text, GuardrailsConfig guardrails, String traceId) {
        // default noop
    }
}
