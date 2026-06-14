package cn.zest.www.zestllm.plugin.cache.caffeine;

import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CacheCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.policy-cache", havingValue = "caffeine", matchIfMissing = true)
    @ConditionalOnMissingBean(PolicyCacheAdapter.class)
    public PolicyCacheAdapter caffeinePolicyCacheAdapter() {
        return new CaffeinePolicyCacheAdapter();
    }
}
