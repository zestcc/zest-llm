package cn.zest.www.zestllm.plugin.noop;

import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopContentModerationAdapter implements ContentModerationAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public void checkPrompt(String prompt, GuardrailsConfig guardrails, String traceId) {
        log.trace("Noop moderation traceId={}", traceId);
    }
}
