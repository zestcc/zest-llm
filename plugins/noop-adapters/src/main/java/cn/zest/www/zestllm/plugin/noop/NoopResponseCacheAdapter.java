package cn.zest.www.zestllm.plugin.noop;

import cn.zest.www.zestllm.spi.cache.CachedResponse;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class NoopResponseCacheAdapter implements ResponseCacheAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public Optional<CachedResponse> get(String cacheKey) {
        return Optional.empty();
    }

    @Override
    public void put(String cacheKey, CachedResponse response, Duration ttl) {
        log.trace("Noop response cache put key={}", cacheKey);
    }

    @Override
    public void invalidate(String appKey, String code) {
        // noop
    }
}
