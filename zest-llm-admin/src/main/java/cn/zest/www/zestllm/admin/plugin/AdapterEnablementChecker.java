package cn.zest.www.zestllm.admin.plugin;

import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI 默认适配器：YAML 配置 + Admin 控制台运行时覆盖（对齐 zest-monitor PluginEnablementChecker）。
 */
@Component
public class AdapterEnablementChecker {

    public static final String DEFAULT_KEY_PREFIX = "default:";

    private final LlmAdapterProperties adapterProperties;
    private final Map<String, String> defaultOverrides = new ConcurrentHashMap<>();

    public AdapterEnablementChecker(LlmAdapterProperties adapterProperties) {
        this.adapterProperties = adapterProperties;
    }

    public void applyDefaultOverrides(Map<String, String> overrides) {
        defaultOverrides.clear();
        if (overrides != null) {
            defaultOverrides.putAll(overrides);
        }
    }

    public String resolveActivePluginId(String spiType) {
        String override = defaultOverrides.get(defaultKey(spiType));
        if (override != null) {
            return override;
        }
        return readFromProperties(spiType);
    }

    public boolean isActive(String spiType, String pluginId) {
        return pluginId.equals(resolveActivePluginId(spiType));
    }

    public static String defaultKey(String spiType) {
        return DEFAULT_KEY_PREFIX + spiType;
    }

    private String readFromProperties(String spiType) {
        return switch (spiType) {
            case "model-gateway" -> adapterProperties.getModelGateway();
            case "observability" -> adapterProperties.getObservability();
            case "agent-runtime" -> adapterProperties.getAgentRuntime();
            case "knowledge-retrieval" -> adapterProperties.getKnowledgeRetrieval();
            case "learning-pipeline" -> adapterProperties.getLearningPipeline();
            case "policy-cache" -> adapterProperties.getPolicyCache();
            case "response-cache" -> adapterProperties.getResponseCache();
            case "quota" -> adapterProperties.getQuota();
            case "audit" -> adapterProperties.getAudit();
            case "prompt-renderer" -> adapterProperties.getPromptRenderer();
            case "output-schema-validator" -> adapterProperties.getOutputSchemaValidator();
            case "report-channel" -> adapterProperties.getReportChannel();
            case "content-moderation" -> adapterProperties.getContentModeration();
            case "alert-webhook" -> adapterProperties.getAlertWebhook();
            case "mcp-tool" -> adapterProperties.getMcpTool();
            default -> "";
        };
    }
}
