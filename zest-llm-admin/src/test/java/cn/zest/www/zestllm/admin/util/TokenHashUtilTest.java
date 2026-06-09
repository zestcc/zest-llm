package cn.zest.www.zestllm.admin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHashUtilTest {

    @Test
    void legacyDemoTokenMatches() {
        assertTrue(TokenHashUtil.matches("demo-token-123",
                "85d35da9c6db00a7d7c61ffbb863b3b94a0b9c6c26f96f3ed4f62ac1b612069f"));
    }

    @Test
    void saltedTokenRoundTrip() {
        String raw = TokenHashUtil.generateRawToken();
        String stored = TokenHashUtil.encodeToken(raw);
        assertTrue(stored.contains("."));
        assertTrue(TokenHashUtil.matches(raw, stored));
        assertFalse(TokenHashUtil.matches(raw + "x", stored));
    }

    @Test
    void traceIdHasPrefix() {
        assertTrue(TokenHashUtil.newTraceId().startsWith("tr_"));
    }
}
