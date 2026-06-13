package cn.zest.www.zestllm.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminProductionGuardTest {

    private AdminProductionGuard guard(MockEnvironment env) {
        return new AdminProductionGuard(env);
    }

    private static MockEnvironment validProdEnv() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("zest-llm.admin.jwt.secret", "prod-jwt-secret-key-at-least-32-chars-long");
        env.setProperty("zest-llm.admin.sso.enabled", "false");
        return env;
    }

    @Test
    void validate_whenSsoDisabled_passesWithValidJwt() {
        assertThatCode(() -> guard(validProdEnv()).validateProductionConfig()).doesNotThrowAnyException();
    }

    @Test
    void validate_whenSsoEnabledWithWeakSecret_fails() {
        MockEnvironment env = validProdEnv();
        env.setProperty("zest-llm.admin.sso.enabled", "true");
        env.setProperty("zest-llm.admin.sso.client-id", "zest-llm-admin");
        env.setProperty("zest-llm.admin.sso.client-secret", "change-me-in-production");
        env.setProperty("zest-llm.admin.sso.redirect-uri", "https://admin.example.com/login/callback");

        assertThatThrownBy(() -> guard(env).validateProductionConfig())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validate_whenSsoEnabledWithValidConfig_passes() {
        MockEnvironment env = validProdEnv();
        env.setProperty("zest-llm.admin.sso.enabled", "true");
        env.setProperty("zest-llm.admin.sso.client-id", "zest-llm-admin");
        env.setProperty("zest-llm.admin.sso.client-secret", "real-production-secret-value");
        env.setProperty("zest-llm.admin.sso.redirect-uri", "https://admin.example.com/login/callback");

        assertThatCode(() -> guard(env).validateProductionConfig()).doesNotThrowAnyException();
    }

    @Test
    void validate_whenSsoEnabledMissingRedirectUri_fails() {
        MockEnvironment env = validProdEnv();
        env.setProperty("zest-llm.admin.sso.enabled", "true");
        env.setProperty("zest-llm.admin.sso.client-id", "zest-llm-admin");
        env.setProperty("zest-llm.admin.sso.client-secret", "real-production-secret-value");

        assertThatThrownBy(() -> guard(env).validateProductionConfig())
                .isInstanceOf(IllegalStateException.class);
    }
}
