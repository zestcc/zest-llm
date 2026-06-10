package cn.zest.www.zestllm.spi.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * LLM 响应语义缓存 SPI（相同 app+task+model+prompt 命中缓存，跳过网关调用）。
 */
public interface ResponseCacheAdapter {

    String adapterId();

    Optional<CachedResponse> get(String cacheKey);

    void put(String cacheKey, CachedResponse response, Duration ttl);

    void invalidate(String appKey, String code);

    static String buildKey(String appKey, String code, String model, String promptHash) {
        return "resp:" + appKey + ":" + code + ":" + model + ":" + promptHash;
    }
}
