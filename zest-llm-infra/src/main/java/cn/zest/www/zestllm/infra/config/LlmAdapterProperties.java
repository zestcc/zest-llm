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
    private String reportChannel = "sync";
    private String contentModeration = "keyword-blocklist";
    private String responseCache = "noop";
    private String alertWebhook = "http";
    /** native | dify | noop — 见 docs/AI整合与自我改进标准-完整版.md */
    private String agentRuntime = "native";
    /** ragflow | dify-kb | noop */
    private String knowledgeRetrieval = "noop";
    /** zest-eval | noop */
    private String learningPipeline = "noop";
    /** http-mcp | noop（路线图） */
    private String mcpTool = "http-mcp";
}
