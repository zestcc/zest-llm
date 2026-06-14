package cn.zest.www.zestllm.plugin.schema.json;

import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SchemaJsonAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.output-schema-validator", havingValue = "json", matchIfMissing = true)
    @ConditionalOnMissingBean(OutputSchemaValidator.class)
    public OutputSchemaValidator jsonOutputSchemaValidator(ObjectMapper objectMapper) {
        return new JsonOutputSchemaValidator(objectMapper);
    }
}
