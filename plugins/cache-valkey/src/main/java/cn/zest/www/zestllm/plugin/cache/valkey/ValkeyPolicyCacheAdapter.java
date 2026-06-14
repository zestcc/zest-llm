package cn.zest.www.zestllm.plugin.cache.valkey;

import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class ValkeyPolicyCacheAdapter implements PolicyCacheAdapter {

    private static final String KEY_PREFIX = "zest:policy:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "valkey";
    }

    @Override
    public Optional<CachedPolicy> getPolicy(String cacheKey) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + cacheKey);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CachedPolicy.class));
        } catch (JsonProcessingException ex) {
            log.warn("Invalid cached policy key={}", cacheKey, ex);
            redisTemplate.delete(KEY_PREFIX + cacheKey);
            return Optional.empty();
        }
    }

    @Override
    public void putPolicy(String cacheKey, CachedPolicy policy, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(policy);
            long seconds = ttl != null && !ttl.isZero() && !ttl.isNegative()
                    ? ttl.getSeconds()
                    : 300;
            redisTemplate.opsForValue().set(KEY_PREFIX + cacheKey, json, seconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize policy key={}", cacheKey, ex);
        }
    }

    @Override
    public void invalidate(String appKey, String code) {
        redisTemplate.delete(KEY_PREFIX + buildKey(appKey, code));
    }

    private static String buildKey(String appKey, String code) {
        return appKey + ":" + code;
    }
}
