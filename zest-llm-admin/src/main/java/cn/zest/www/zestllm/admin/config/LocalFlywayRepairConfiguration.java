package cn.zest.www.zestllm.admin.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 本地开发：Flyway 失败迁移自动 repair 后重跑，避免半失败 V33 阻断 Admin 启动。
 */
@Configuration
@Profile("local")
public class LocalFlywayRepairConfiguration {

    @Bean
    public FlywayMigrationStrategy localFlywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
