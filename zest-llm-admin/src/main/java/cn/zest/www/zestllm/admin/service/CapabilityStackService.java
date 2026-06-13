package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.AdapterHealthVO;
import cn.zest.www.zestllm.admin.model.vo.CapabilityStackVO;
import cn.zest.www.zestllm.admin.model.vo.StackTierVO;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CapabilityStackService {

    private final AdapterHealthService adapterHealthService;
    private final LlmAdapterProperties adapterProperties;

    @Value("${zest.stack.tier:small}")
    private String stackTier;

    public CapabilityStackVO overview() {
        String tier = normalizeTier(stackTier);
        List<StackTierVO> tiers = List.of(smallTier(), mediumTier(), largeTier());
        Map<String, String> recommended = recommendedForTier(tier);
        return CapabilityStackVO.builder()
                .currentTier(tier)
                .deployCommand(deployCommand(tier))
                .tiers(tiers)
                .adapters(adapterHealthService.listAll())
                .recommendedAdapters(recommended)
                .build();
    }

    public StackTierVO getTier(String tierId) {
        return switch (normalizeTier(tierId)) {
            case "medium" -> mediumTier();
            case "large" -> largeTier();
            default -> smallTier();
        };
    }

    private String normalizeTier(String tier) {
        if (tier == null) {
            return "small";
        }
        String t = tier.trim().toLowerCase();
        return switch (t) {
            case "medium", "med", "m" -> "medium";
            case "large", "lg", "l" -> "large";
            default -> "small";
        };
    }

    private StackTierVO smallTier() {
        return StackTierVO.builder()
                .id("small")
                .name("小型 · POC")
                .description("单机验证：LiteLLM + Admin + Demo，适合 1～2 个 AI 作业")
                .components(List.of("MySQL", "LiteLLM", "openai-mock", "Admin", "Demo", "mcp-mock"))
                .adapterDefaults(Map.of(
                        "model-gateway", "litellm",
                        "observability", "noop",
                        "agent-runtime", "native",
                        "knowledge-retrieval", "noop",
                        "learning-pipeline", "noop",
                        "response-cache", "caffeine"))
                .expectedQps("~50 prepare/s")
                .composeHint("zest-stack-up.ps1 -Tier small")
                .build();
    }

    private StackTierVO mediumTier() {
        return StackTierVO.builder()
                .id("medium")
                .name("中型 · 部门级")
                .description("Langfuse 可观测 + Valkey 缓存 + Eval 门禁，适合 5～20 个作业")
                .components(List.of("small 全部", "Valkey", "Langfuse", "ZestFlow"))
                .adapterDefaults(Map.of(
                        "model-gateway", "litellm",
                        "observability", "langfuse",
                        "agent-runtime", "native",
                        "knowledge-retrieval", "noop",
                        "learning-pipeline", "zest-eval",
                        "response-cache", "valkey"))
                .expectedQps("~200 prepare/s")
                .composeHint("zest-stack-up.ps1 -Tier medium")
                .build();
    }

    private StackTierVO largeTier() {
        return StackTierVO.builder()
                .id("large")
                .name("大型 · 企业整合")
                .description("Dify 编排 + RAGFlow 知识 + Kafka 异步 report，适合高并发多步 Agent")
                .components(List.of("medium 全部", "Kafka/Redpanda", "Dify", "RAGFlow"))
                .adapterDefaults(Map.of(
                        "model-gateway", "litellm",
                        "observability", "langfuse",
                        "agent-runtime", "dify",
                        "knowledge-retrieval", "ragflow",
                        "learning-pipeline", "zest-eval",
                        "report-channel", "kafka"))
                .expectedQps("500+ prepare/s（多 Admin 副本）")
                .composeHint("zest-stack-up.ps1 -Tier large")
                .build();
    }

    private Map<String, String> recommendedForTier(String tier) {
        Map<String, String> defaults = new LinkedHashMap<>(getTier(tier).getAdapterDefaults());
        defaults.put("model-gateway-active", adapterProperties.getModelGateway());
        defaults.put("observability-active", adapterProperties.getObservability());
        defaults.put("agent-runtime-active", adapterProperties.getAgentRuntime());
        defaults.put("knowledge-retrieval-active", adapterProperties.getKnowledgeRetrieval());
        defaults.put("learning-pipeline-active", adapterProperties.getLearningPipeline());
        return defaults;
    }

    private String deployCommand(String tier) {
        return switch (tier) {
            case "medium" -> "powershell -File deploy/scripts/zest-stack-up.ps1 -Tier medium";
            case "large" -> "powershell -File deploy/scripts/zest-stack-up.ps1 -Tier large";
            default -> "powershell -File deploy/scripts/zest-stack-up.ps1 -Tier small";
        };
    }

    public Map<String, String> exportComposeEnv(String tierId) {
        StackTierVO tier = getTier(tierId);
        Map<String, String> env = new LinkedHashMap<>();
        env.put("ZEST_STACK_TIER", tier.getId());
        tier.getAdapterDefaults().forEach((key, value) ->
                env.put("ZEST_LLM_ADAPTERS_" + key.toUpperCase().replace('-', '_'), value));
        if ("large".equals(tier.getId())) {
            env.put("DIFY_API_BASE", "http://dify-api:5001");
            env.put("RAGFLOW_API_BASE", "http://ragflow:9380");
            env.put("integrationCompose", "docker-compose.integration.yml --profile integration");
            env.put("integrationDemo", "bash deploy/scripts/integration-demo.sh");
        }
        env.put("deployCommand", deployCommand(tier.getId()));
        env.put("composeHint", tier.getComposeHint());
        return env;
    }
}
