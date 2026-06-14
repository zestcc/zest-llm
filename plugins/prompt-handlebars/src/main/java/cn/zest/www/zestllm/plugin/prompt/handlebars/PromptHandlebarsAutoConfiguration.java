package cn.zest.www.zestllm.plugin.prompt.handlebars;

import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PromptHandlebarsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.prompt-renderer", havingValue = "handlebars", matchIfMissing = true)
    @ConditionalOnMissingBean(PromptRenderer.class)
    public PromptRenderer handlebarsPromptRenderer() {
        return new HandlebarsPromptRenderer();
    }
}
