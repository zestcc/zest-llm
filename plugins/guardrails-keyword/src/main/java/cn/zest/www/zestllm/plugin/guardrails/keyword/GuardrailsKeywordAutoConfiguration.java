package cn.zest.www.zestllm.plugin.guardrails.keyword;

import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class GuardrailsKeywordAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.content-moderation", havingValue = "keyword-blocklist", matchIfMissing = true)
    @ConditionalOnMissingBean(ContentModerationAdapter.class)
    public ContentModerationAdapter keywordBlocklistModerationAdapter() {
        return new KeywordBlocklistModerationAdapter();
    }
}
