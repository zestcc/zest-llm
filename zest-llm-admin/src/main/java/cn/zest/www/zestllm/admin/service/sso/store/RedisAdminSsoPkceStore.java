package cn.zest.www.zestllm.admin.service.sso.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 集群 PKCE 存储 — 配置 {@code spring.data.redis.host} 时启用。
 */
@RequiredArgsConstructor
public class RedisAdminSsoPkceStore implements AdminSsoPkceStore {

    private static final String PREFIX = "zest-llm:sso:pkce:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String state, String codeVerifier) {
        redisTemplate.opsForValue().set(PREFIX + state, codeVerifier, TTL.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public String consume(String state) {
        String key = PREFIX + state;
        String verifier = redisTemplate.opsForValue().get(key);
        if (verifier != null) {
            redisTemplate.delete(key);
        }
        return verifier;
    }
}
