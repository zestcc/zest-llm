package cn.zest.www.zestllm.admin.config;

import cn.zest.www.zestllm.admin.service.sso.store.AdminSsoPkceStore;
import cn.zest.www.zestllm.admin.service.sso.store.InMemoryAdminSsoPkceStore;
import cn.zest.www.zestllm.admin.service.sso.store.RedisAdminSsoPkceStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * PKCE 存储装配 — 无 Redis 时用内存；配置 {@code spring.data.redis.host} 时用 Redis。
 */
@Configuration
public class AdminSsoStoreConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    AdminSsoPkceStore redisAdminSsoPkceStore(StringRedisTemplate redisTemplate) {
        return new RedisAdminSsoPkceStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(AdminSsoPkceStore.class)
    AdminSsoPkceStore inMemoryAdminSsoPkceStore() {
        return new InMemoryAdminSsoPkceStore();
    }
}
