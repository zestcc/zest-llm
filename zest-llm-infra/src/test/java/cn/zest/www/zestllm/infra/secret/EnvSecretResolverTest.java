package cn.zest.www.zestllm.infra.secret;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvSecretResolverTest {

    private final EnvSecretResolver resolver = new EnvSecretResolver();

    @Test
    void resolve_readsFromSystemProperty() {
        System.setProperty("ZEST_TEST_KEY", "secret-value");
        try {
            assertEquals("secret-value", resolver.resolve("env:ZEST_TEST_KEY").orElseThrow());
        } finally {
            System.clearProperty("ZEST_TEST_KEY");
        }
    }

    @Test
    void resolve_returnsEmptyForUnknownRef() {
        assertTrue(resolver.resolve("vault:missing").isEmpty());
    }
}
