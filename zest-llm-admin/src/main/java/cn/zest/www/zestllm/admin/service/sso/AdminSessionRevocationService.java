package cn.zest.www.zestllm.admin.service.sso;

import cn.zest.www.zestllm.spi.adminsso.AdminSsoSessionRevocation;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * SSO Back-Channel 登出后吊销本地 JWT（按用户名）。需配置 {@code spring.data.redis.host}。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class AdminSessionRevocationService implements AdminSsoSessionRevocation {

    private static final String PREFIX = "zest-llm:admin:sso-logout:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public void revokeByUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        redisTemplate.opsForValue().set(PREFIX + username, "1", TTL);
    }

    public void clearRevocation(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        redisTemplate.delete(PREFIX + username);
    }

    public boolean isRevoked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + username));
    }
}
