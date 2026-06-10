package cn.zest.www.zestllm.common.api;

import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;
import cn.zest.www.zestllm.spi.profile.ProviderDefinition;
import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class PrepareResponse {
    private String traceId;
    private String code;
    private String promptVersion;
    private String profileVersion;
    private String renderedPrompt;
    private String model;
    private List<String> fallbackModels;
    private Integer maxTokens;
    private Double temperature;
    private Integer timeoutMs;
    private String outputSchema;
    /** invoke | agent */
    private String runtimeMode;
    private String providerRef;
    private String gatewayBaseUrl;
    private String gatewayProtocol;
    private String inboundAuthMode;
    private String outboundAuthMode;
    /** Reference only, resolved on agent side via SecretResolver */
    private String outboundSecretRef;
    private List<ToolDefinition> tools;
    /** loop | prefetch */
    private String toolCallMode;
    private Integer maxToolSteps;
    private GuardrailsConfig guardrails;
    /** Provider definitions without secrets, for agent-side routing */
    private Map<String, ProviderDefinition> providers = new LinkedHashMap<>();
}
