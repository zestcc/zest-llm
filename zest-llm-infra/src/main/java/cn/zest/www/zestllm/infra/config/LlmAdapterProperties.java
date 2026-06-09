package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.adapters")
public class LlmAdapterProperties {
    private String modelGateway = "litellm";
    private String observability = "noop";
    private String promptRenderer = "handlebars";
    private String policyCache = "caffeine";
    private String quota = "noop";
    private String outputSchemaValidator = "json";
    private String audit = "noop";
}
