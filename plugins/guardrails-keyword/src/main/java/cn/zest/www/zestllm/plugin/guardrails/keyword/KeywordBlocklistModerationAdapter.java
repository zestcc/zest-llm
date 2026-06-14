package cn.zest.www.zestllm.plugin.guardrails.keyword;

import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;

import java.util.List;
import java.util.Locale;

/**
 * Keyword blocklist moderation for common jailbreak phrases.
 */
public class KeywordBlocklistModerationAdapter implements ContentModerationAdapter {

    private static final List<String> DEFAULT_BLOCKED = List.of(
            "ignore previous instructions",
            "ignore all previous",
            "disregard your instructions",
            "jailbreak",
            "dan mode"
    );

    @Override
    public String adapterId() {
        return "keyword-blocklist";
    }

    @Override
    public void checkPrompt(String prompt, GuardrailsConfig guardrails, String traceId) {
        if (prompt == null || guardrails == null || !Boolean.TRUE.equals(guardrails.getBlockPromptKeywords())) {
            return;
        }
        List<String> blocked = guardrails.getBlockedKeywords();
        if (blocked == null || blocked.isEmpty()) {
            blocked = DEFAULT_BLOCKED;
        }
        String lower = prompt.toLowerCase(Locale.ROOT);
        for (String keyword : blocked) {
            if (keyword != null && !keyword.isBlank() && lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                throw new ZestLlmException(LlmErrorCode.POLICY_VIOLATION, traceId,
                        "Prompt blocked by keyword policy");
            }
        }
    }

    @Override
    public void checkOutput(String text, GuardrailsConfig guardrails, String traceId) {
        checkPrompt(text, guardrails, traceId);
    }
}
