package cn.zest.www.zestllm.plugin.cache.valkey;

import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnExpression("'${zest.llm.adapters.policy-cache:}' == 'valkey' || '${zest.llm.adapters.response-cache:}' == 'valkey'")
public class CacheValkeyAutoConfiguration {

    @Bean
    @ConditionalOnExpression("'${zest.llm.adapters.policy-cache:}' == 'valkey' || '${zest.llm.adapters.response-cache:}' == 'valkey'")
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.response-cache", havingValue = "valkey")
    @ConditionalOnMissingBean(ResponseCacheAdapter.class)
    public ResponseCacheAdapter valkeyResponseCacheAdapter(StringRedisTemplate redisTemplate,
                                                           ObjectMapper objectMapper) {
        return new ValkeyResponseCacheAdapter(redisTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.policy-cache", havingValue = "valkey")
    @ConditionalOnMissingBean(PolicyCacheAdapter.class)
    public PolicyCacheAdapter valkeyPolicyCacheAdapter(StringRedisTemplate redisTemplate,
                                                      ObjectMapper objectMapper) {
        return new ValkeyPolicyCacheAdapter(redisTemplate, objectMapper);
    }
}
