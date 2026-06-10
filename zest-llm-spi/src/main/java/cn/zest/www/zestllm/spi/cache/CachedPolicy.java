package cn.zest.www.zestllm.spi.cache;

import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;
import cn.zest.www.zestllm.spi.profile.ProviderDefinition;
import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CachedPolicy {
    private String promptVersion;
    private String profileVersion;
    private String templateBody;
    private String outputSchema;
    private String primaryModel;
    private List<String> fallbackModels;
    private Integer maxTokens;
    private Double temperature;
    private Integer timeoutMs;
    private String runtimeMode;
    private String providerRef;
    private String gatewayBaseUrl;
    private String gatewayProtocol;
    private String inboundAuthMode;
    private String outboundAuthMode;
    private String outboundSecretRef;
    private List<ToolDefinition> tools;
    /** loop | prefetch */
    private String toolCallMode;
    private Integer maxToolSteps;
    private GuardrailsConfig guardrails;
    @Builder.Default
    private Map<String, ProviderDefinition> providers = new LinkedHashMap<>();
    /** 完整 Profile 快照（缓存用，不含密钥） */
    private AgentProfileDocument profileDocument;
}
