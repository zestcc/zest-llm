package cn.zest.www.zestllm.spi.cache;

import java.time.Duration;
import java.util.Optional;

public interface PolicyCacheAdapter {

    String adapterId();

    Optional<CachedPolicy> getPolicy(String cacheKey);

    void putPolicy(String cacheKey, CachedPolicy policy, Duration ttl);

    void invalidate(String appKey, String code);
}
