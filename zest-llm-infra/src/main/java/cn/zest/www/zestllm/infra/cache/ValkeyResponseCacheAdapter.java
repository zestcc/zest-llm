package cn.zest.www.zestllm.infra.cache;

import cn.zest.www.zestllm.spi.cache.CachedResponse;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class ValkeyResponseCacheAdapter implements ResponseCacheAdapter {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "zestllm:resp:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "valkey";
    }

    @Override
    public Optional<CachedResponse> get(String cacheKey) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + cacheKey);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CachedResponse.class));
        } catch (JsonProcessingException ex) {
            log.warn("Invalid response cache entry key={}", cacheKey);
            return Optional.empty();
        }
    }

    @Override
    public void put(String cacheKey, CachedResponse response, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(response);
            Duration effective = ttl != null ? ttl : DEFAULT_TTL;
            redisTemplate.opsForValue().set(KEY_PREFIX + cacheKey, json, effective);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize response cache key={}", cacheKey, ex);
        }
    }

    @Override
    public void invalidate(String appKey, String code) {
        String scanPattern = KEY_PREFIX + "resp:" + appKey + ":" + code + ":*";
        Set<String> matched = redisTemplate.keys(scanPattern);
        if (matched != null && !matched.isEmpty()) {
            redisTemplate.delete(matched);
        }
    }

    public static String hashPrompt(String prompt) {
        if (prompt == null) {
            return "empty";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(prompt.hashCode());
        }
    }
}
