package cn.zest.www.zestllm.infra.guardrails;

import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardrailsEnforcerTest {

    @Test
    void sanitizePrompt_redactsPiiWhenEnabled() {
        GuardrailsConfig config = new GuardrailsConfig();
        config.setPiiRedact(true);
        String input = "Contact user@example.com or 13800138000 id 110101199001011234";
        String sanitized = GuardrailsEnforcer.sanitizePrompt(input, config);
        assertFalse(sanitized.contains("user@example.com"));
        assertFalse(sanitized.contains("13800138000"));
        assertTrue(sanitized.contains("[REDACTED_EMAIL]"));
        assertTrue(sanitized.contains("[REDACTED_PHONE]"));
    }

    @Test
    void sanitizeOutput_redactsStringValues() {
        GuardrailsConfig config = new GuardrailsConfig();
        config.setPiiRedact(true);
        Map<String, Object> output = GuardrailsEnforcer.sanitizeOutput(
                Map.of("answer", "email: admin@test.com"), config);
        assertTrue(output.get("answer").toString().contains("[REDACTED_EMAIL]"));
    }

    @Test
    void shouldValidateSchema_respectsGuardrailsFlag() {
        GuardrailsConfig off = new GuardrailsConfig();
        off.setBlockOnSchemaMismatch(false);
        assertFalse(GuardrailsEnforcer.shouldValidateSchema(off));
        assertTrue(GuardrailsEnforcer.shouldValidateSchema(new GuardrailsConfig()));
    }
}
