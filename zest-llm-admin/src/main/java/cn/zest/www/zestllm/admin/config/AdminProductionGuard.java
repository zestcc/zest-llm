package cn.zest.www.zestllm.admin.config;

import cn.zest.www.zestllm.common.util.ProductionSecretGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 生产 profile 安全校验 — SSO 密钥、JWT 等硬性要求。
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class AdminProductionGuard {

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionConfig() {
        boolean failed = false;

        if (ProductionSecretGuard.isDefaultJwtSecret(environment.getProperty("zest-llm.admin.jwt.secret"))) {
            log.error("[prod] zest-llm.admin.jwt.secret 仍为默认值、过短或未配置（至少 32 字符）");
            failed = true;
        }

        if (Boolean.TRUE.equals(environment.getProperty("zest-llm.admin.sso.enabled", Boolean.class, Boolean.FALSE))) {
            if (ProductionSecretGuard.isWeakOAuthClientSecret(environment.getProperty("zest-llm.admin.sso.client-secret"))) {
                log.error("[prod] zest-llm.admin.sso.enabled=true 时 client-secret 须更换模板占位符");
                failed = true;
            }
            if (!ProductionSecretGuard.hasText(environment.getProperty("zest-llm.admin.sso.redirect-uri"))) {
                log.error("[prod] zest-llm.admin.sso.enabled=true 时必须配置 redirect-uri");
                failed = true;
            }
            if (!ProductionSecretGuard.hasText(environment.getProperty("zest-llm.admin.sso.client-id"))) {
                log.error("[prod] zest-llm.admin.sso.enabled=true 时必须配置 client-id");
                failed = true;
            }
        }

        if (failed) {
            throw new IllegalStateException("生产环境配置不完整，请修正上述 [prod] 日志项后重启");
        }
        log.info("[prod] 安全配置校验通过");
    }
}
