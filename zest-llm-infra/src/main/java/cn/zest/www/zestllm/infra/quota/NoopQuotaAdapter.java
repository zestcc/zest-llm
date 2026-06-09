package cn.zest.www.zestllm.infra.quota;

import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;

public class NoopQuotaAdapter implements QuotaAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public void checkAndConsume(Long appId, int estimatedTokens) {
        // always pass
    }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("noop").build();
    }
}
