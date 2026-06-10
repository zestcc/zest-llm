package cn.zest.www.zestllm.agent.cache;

import cn.zest.www.zestllm.common.api.PrepareResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 侧 Prepare 结果本地缓存，CP 不可用时降级使用。
 */
public class AgentPolicyCache {

    private final Duration ttl;
    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

    public AgentPolicyCache(Duration ttl) {
        this.ttl = ttl != null && !ttl.isNegative() && !ttl.isZero() ? ttl : Duration.ofSeconds(300);
    }

    public Optional<PrepareResponse> get(String appKey, String code) {
        if (appKey == null || code == null) {
            return Optional.empty();
        }
        CacheEntry entry = entries.get(cacheKey(appKey, code));
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            if (entry != null) {
                entries.remove(cacheKey(appKey, code));
            }
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    public void put(String appKey, String code, PrepareResponse response) {
        if (appKey == null || code == null || response == null) {
            return;
        }
        entries.put(cacheKey(appKey, code), new CacheEntry(response, Instant.now().plus(ttl)));
    }

    public void invalidate(String appKey, String code) {
        if (appKey != null && code != null) {
            entries.remove(cacheKey(appKey, code));
        }
    }

    private String cacheKey(String appKey, String code) {
        return appKey + ":" + code;
    }

    private record CacheEntry(PrepareResponse response, Instant expiresAt) {
    }
}
