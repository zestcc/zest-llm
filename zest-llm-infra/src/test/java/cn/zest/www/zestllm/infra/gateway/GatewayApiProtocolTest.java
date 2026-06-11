package cn.zest.www.zestllm.infra.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayApiProtocolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizeMapsOpenAiAliases() {
        assertEquals("openai", GatewayApiProtocol.normalize(null));
        assertEquals("openai", GatewayApiProtocol.normalize("openai-compatible"));
        assertEquals("anthropic", GatewayApiProtocol.normalize("anthropic"));
        assertEquals("anthropic", GatewayApiProtocol.normalize("anthropic-messages"));
    }

    @Test
    void extractAnthropicTextFromBlocks() throws Exception {
        var content = objectMapper.readTree("""
                [{"type":"text","text":"hello world"}]
                """);
        assertEquals("hello world", LiteLLMGatewayAdapter.extractAnthropicText(content));
    }

    @Test
    void isAnthropic() {
        assertTrue(GatewayApiProtocol.isAnthropic("anthropic"));
        assertFalse(GatewayApiProtocol.isAnthropic("openai"));
    }

    @Test
    void resolveEffectivePrefersModelLevel() {
        assertEquals("anthropic", GatewayApiProtocol.resolveEffective("anthropic", "openai", "openai"));
        assertEquals("openai", GatewayApiProtocol.resolveEffective(null, "openai", "anthropic"));
        assertEquals("anthropic", GatewayApiProtocol.resolveEffective(null, null, "anthropic"));
    }
}
