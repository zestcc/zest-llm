package cn.zest.www.zestllm.spi.quota;

import cn.zest.www.zestllm.spi.model.HealthStatus;

public interface QuotaAdapter {

    String adapterId();

    void checkAndConsume(Long appId, int estimatedTokens);

    HealthStatus health();
}
