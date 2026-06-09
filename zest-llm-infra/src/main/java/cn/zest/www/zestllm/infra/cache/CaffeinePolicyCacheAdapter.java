package cn.zest.www.zestllm.infra.cache;

import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class CaffeinePolicyCacheAdapter implements PolicyCacheAdapter {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final Cache<String, CachedPolicy> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(DEFAULT_TTL)
            .build();

    @Override
    public String adapterId() {
        return "caffeine";
    }

    @Override
    public Optional<CachedPolicy> getPolicy(String cacheKey) {
        return Optional.ofNullable(cache.getIfPresent(cacheKey));
    }

    @Override
    public void putPolicy(String cacheKey, CachedPolicy policy, Duration ttl) {
        cache.put(cacheKey, policy);
        log.debug("Cached policy key={} ttl={}", cacheKey, ttl);
    }

    @Override
    public void invalidate(String appKey, String code) {
        cache.invalidate(buildKey(appKey, code));
    }

    public static String buildKey(String appKey, String code) {
        return appKey + ":" + code;
    }
}
