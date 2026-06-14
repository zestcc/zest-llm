package cn.zest.www.zestllm.plugin.knowledge.http;

import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(HttpKnowledgeProperties.class)
public class KnowledgeHttpAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.knowledge-retrieval", havingValue = "http-knowledge")
    public KnowledgeRetrievalAdapter httpKnowledgeRetrievalAdapter(HttpKnowledgeProperties properties,
                                                                  SecretResolver secretResolver,
                                                                  ObjectMapper objectMapper) {
        return new HttpKnowledgeRetrievalAdapter(properties, secretResolver, objectMapper);
    }
}
