package cn.zest.www.zestllm.admin.adapter;

import cn.zest.www.zestllm.admin.mapper.LlmExecutionMapper;
import cn.zest.www.zestllm.admin.repo.LlmAppQuotaRepo;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "zest.llm.adapters.quota", havingValue = "db")
public class DbQuotaAdapter implements QuotaAdapter {

    private final LlmAppQuotaRepo quotaRepo;
    private final LlmExecutionMapper executionMapper;

    @Override
    public String adapterId() {
        return "db";
    }

    @Override
    public void checkAndConsume(Long appId, int estimatedTokens) {
        var quota = quotaRepo.findByAppId(appId).orElse(null);
        if (quota == null || quota.getDailyTokenLimit() == null) {
            return;
        }
        long usedToday = executionMapper.sumTodayTokensByAppId(appId);
        if (usedToday + estimatedTokens > quota.getDailyTokenLimit()) {
            throw new ZestLlmException(LlmErrorCode.QUOTA_EXCEEDED);
        }
    }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("db").build();
    }
}
