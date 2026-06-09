package cn.zest.www.zestllm.admin.adapter;

import cn.zest.www.zestllm.admin.mapper.LlmExecutionMapper;
import cn.zest.www.zestllm.admin.repo.LlmAppQuotaRepo;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "zest.llm.adapters.quota", havingValue = "redis-token-bucket")
public class RedisTokenBucketQuotaAdapter implements QuotaAdapter {

    private static final String QPS_KEY_PREFIX = "zest:qps:";

    private final LlmAppQuotaRepo quotaRepo;
    private final LlmExecutionMapper executionMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String adapterId() {
        return "redis-token-bucket";
    }

    @Override
    public void checkAndConsume(Long appId, int estimatedTokens) {
        var quota = quotaRepo.findByAppId(appId).orElse(null);
        if (quota != null && quota.getDailyTokenLimit() != null) {
            long usedToday = executionMapper.sumTodayTokensByAppId(appId);
            if (usedToday + estimatedTokens > quota.getDailyTokenLimit()) {
                throw new ZestLlmException(LlmErrorCode.QUOTA_EXCEEDED);
            }
        }
        if (quota != null && quota.getQpsLimit() != null && quota.getQpsLimit() > 0) {
            String key = QPS_KEY_PREFIX + appId + ":" + Instant.now().getEpochSecond();
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, 2, TimeUnit.SECONDS);
            }
            if (count != null && count > quota.getQpsLimit()) {
                throw new ZestLlmException(LlmErrorCode.QUOTA_EXCEEDED, null, "QPS 超限");
            }
        }
    }

    @Override
    public HealthStatus health() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return HealthStatus.builder().up(true).message("valkey").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message(ex.getMessage()).build();
        }
    }
}
