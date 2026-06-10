package cn.zest.www.zestllm.infra.guardrails;

import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Profile guardrails 执行：PII 脱敏、Prompt 长度限制。
 */
public final class GuardrailsEnforcer {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{17}[\\dXx]|\\d{15})(?!\\d)");

    private GuardrailsEnforcer() {
    }

    public static String sanitizePrompt(String prompt, GuardrailsConfig guardrails) {
        if (prompt == null || guardrails == null || !Boolean.TRUE.equals(guardrails.getPiiRedact())) {
            return prompt;
        }
        String sanitized = EMAIL.matcher(prompt).replaceAll("[REDACTED_EMAIL]");
        sanitized = PHONE.matcher(sanitized).replaceAll("[REDACTED_PHONE]");
        sanitized = ID_CARD.matcher(sanitized).replaceAll("[REDACTED_ID]");
        return sanitized;
    }

    public static void checkPromptLength(String prompt, GuardrailsConfig guardrails, String traceId) {
        if (guardrails == null || guardrails.getMaxPromptLength() == null || prompt == null) {
            return;
        }
        if (prompt.length() > guardrails.getMaxPromptLength()) {
            throw new ZestLlmException(LlmErrorCode.POLICY_VIOLATION, traceId,
                    "Prompt exceeds max length " + guardrails.getMaxPromptLength());
        }
    }

    public static Map<String, Object> sanitizeOutput(Map<String, Object> output, GuardrailsConfig guardrails) {
        if (output == null || guardrails == null || !Boolean.TRUE.equals(guardrails.getPiiRedact())) {
            return output;
        }
        Map<String, Object> copy = new java.util.LinkedHashMap<>(output);
        copy.replaceAll((k, v) -> v instanceof String s ? sanitizePrompt(s, guardrails) : v);
        return copy;
    }

    public static boolean shouldValidateSchema(GuardrailsConfig guardrails) {
        return guardrails == null || !Boolean.FALSE.equals(guardrails.getBlockOnSchemaMismatch());
    }

    public static String enforcePrompt(String prompt,
                                       GuardrailsConfig guardrails,
                                       String traceId,
                                       ContentModerationAdapter moderation) {
        String sanitized = sanitizePrompt(prompt, guardrails);
        checkPromptLength(sanitized, guardrails, traceId);
        if (moderation != null) {
            moderation.checkPrompt(sanitized, guardrails, traceId);
        }
        return sanitized;
    }

    public static Map<String, Object> enforceOutput(Map<String, Object> output,
                                                    GuardrailsConfig guardrails,
                                                    String traceId,
                                                    ContentModerationAdapter moderation) {
        Map<String, Object> sanitized = sanitizeOutput(output, guardrails);
        if (moderation != null && sanitized != null) {
            for (Object value : sanitized.values()) {
                if (value instanceof String text) {
                    moderation.checkOutput(text, guardrails, traceId);
                }
            }
        }
        return sanitized;
    }
}
