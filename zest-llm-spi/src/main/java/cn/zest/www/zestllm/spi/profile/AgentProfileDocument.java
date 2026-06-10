package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent profile document (CC Switch-style provider + shared snippets model).
 */
@Data
public class AgentProfileDocument {

    public static final String API_VERSION = "zestllm/v1";

    private String apiVersion = API_VERSION;
    private String runtimeMode = "invoke";
    private String providerRef;
    private ModelConfig model;
    private GenerationConfig generation;
    private List<ToolDefinition> tools;
    /** loop: OpenAI function calling 多轮；prefetch: MCP 结果预注入 Prompt */
    private String toolCallMode = "loop";
    private GuardrailsConfig guardrails;
    private Map<String, ProviderDefinition> providers = new LinkedHashMap<>();
    private InboundAuthConfig inboundAuth;
    private OutboundAuthConfig outboundAuth;
    private Map<String, Object> extensions = new LinkedHashMap<>();
}
