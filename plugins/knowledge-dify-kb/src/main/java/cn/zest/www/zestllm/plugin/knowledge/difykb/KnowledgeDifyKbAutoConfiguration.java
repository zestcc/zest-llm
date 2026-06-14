package cn.zest.www.zestllm.plugin.knowledge.difykb;

import cn.zest.www.zestllm.plugin.dify.common.DifyProperties;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class KnowledgeDifyKbAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.knowledge-retrieval", havingValue = "dify-kb")
    public KnowledgeRetrievalAdapter difyKbKnowledgeRetrievalAdapter(DifyProperties properties,
                                                                    SecretResolver secretResolver,
                                                                    ObjectMapper objectMapper) {
        return new DifyKbKnowledgeRetrievalAdapter(properties, secretResolver, objectMapper);
    }
}
