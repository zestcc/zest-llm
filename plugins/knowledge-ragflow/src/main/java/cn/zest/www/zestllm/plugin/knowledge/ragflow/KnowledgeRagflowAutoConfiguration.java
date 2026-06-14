package cn.zest.www.zestllm.plugin.knowledge.ragflow;

import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(RagflowProperties.class)
public class KnowledgeRagflowAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.knowledge-retrieval", havingValue = "ragflow")
    public KnowledgeRetrievalAdapter ragflowKnowledgeRetrievalAdapter(RagflowProperties properties,
                                                                     SecretResolver secretResolver,
                                                                     ObjectMapper objectMapper) {
        return new RagflowKnowledgeRetrievalAdapter(properties, secretResolver, objectMapper);
    }
}
