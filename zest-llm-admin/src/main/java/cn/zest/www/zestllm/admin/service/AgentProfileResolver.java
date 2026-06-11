package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.entity.LlmMcpServerDO;
import cn.zest.www.zestllm.admin.model.entity.LlmProviderPresetDO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.admin.repo.LlmMcpServerRepo;
import cn.zest.www.zestllm.admin.repo.LlmProviderPresetRepo;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import cn.zest.www.zestllm.admin.util.FallbackModelsParser;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.GenerationConfig;
import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import cn.zest.www.zestllm.spi.profile.ModelConfig;
import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import cn.zest.www.zestllm.spi.profile.ProviderDefinition;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProfileResolver {

    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmPromptVersionRepo promptVersionRepo;
    private final LlmModelRouteRepo modelRouteRepo;
    private final LlmProviderPresetRepo providerPresetRepo;
    private final LlmMcpServerRepo mcpServerRepo;
    private final RuntimeAuthService runtimeAuthService;
    private final ObjectMapper objectMapper;

    public CachedPolicy resolve(LlmAppDO app, LlmAiTaskDefDO task, String traceId) {
        return resolve(app, task, null, traceId);
    }

    /**
     * @param profileOverride 非空时使用指定 Profile 版本（用于探测草稿），否则取已发布版
     */
    public CachedPolicy resolve(LlmAppDO app, LlmAiTaskDefDO task, LlmAgentProfileDO profileOverride, String traceId) {
        LlmPromptVersionDO prompt = promptVersionRepo.findPublishedByTaskId(task.getId())
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.PROMPT_NOT_FOUND, traceId));
        LlmModelRouteDO route = modelRouteRepo.findActiveByTaskId(task.getId())
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.INTERNAL_ERROR, traceId, "Model route not configured"));

        Optional<LlmAgentProfileDO> publishedProfile = profileOverride != null
                ? Optional.of(profileOverride)
                : agentProfileRepo.findPublishedByTaskId(task.getId());
        AgentProfileDocument document = publishedProfile
                .map(p -> parseProfile(p.getProfileJson(), traceId))
                .orElse(buildLegacyDocument(route));

        mergeLegacy(document, prompt, route);
        enrichProviders(document, publishedProfile.orElse(null));
        enrichTools(document);
        applyPolicyJsonOverrides(document, route.getPolicyJson());

        InboundAuthConfig inboundAuth = runtimeAuthService.resolveInboundAuth(app);
        if (document.getInboundAuth() == null) {
            document.setInboundAuth(inboundAuth);
        }

        ProviderDefinition activeProvider = resolveActiveProvider(document, publishedProfile.orElse(null));

        return CachedPolicy.builder()
                .promptVersion(prompt.getVersion())
                .profileVersion(publishedProfile.map(LlmAgentProfileDO::getVersion).orElse(null))
                .templateBody(prompt.getTemplateBody())
                .outputSchema(prompt.getOutputSchema())
                .primaryModel(firstNonBlank(document.getModel() != null ? document.getModel().getPrimary() : null,
                        route.getPrimaryModel()))
                .fallbackModels(resolveFallback(document, route))
                .maxTokens(firstNonNull(generation(document).getMaxTokens(), route.getMaxTokens()))
                .temperature(firstNonNull(generation(document).getTemperature(),
                        route.getTemperature() != null ? route.getTemperature().doubleValue() : null))
                .timeoutMs(firstNonNull(generation(document).getTimeoutMs(), route.getTimeoutMs()))
                .runtimeMode(firstNonBlank(document.getRuntimeMode(), "invoke"))
                .providerRef(document.getProviderRef())
                .gatewayBaseUrl(activeProvider != null ? activeProvider.getBaseUrl() : null)
                .gatewayProtocol(activeProvider != null ? activeProvider.getProtocol() : "openai")
                .modelApiProtocol(document.getModel() != null ? document.getModel().getApiProtocol() : null)
                .inboundAuthMode(inboundAuth.getMode())
                .outboundAuthMode(document.getOutboundAuth() != null ? document.getOutboundAuth().getMode() : null)
                .outboundSecretRef(document.getOutboundAuth() != null ? document.getOutboundAuth().getSecretRef() : null)
                .tools(document.getTools())
                .toolCallMode(firstNonBlank(document.getToolCallMode(), "loop"))
                .maxToolSteps(firstNonNull(generation(document).getMaxToolSteps(), 5))
                .guardrails(document.getGuardrails() != null ? document.getGuardrails() : defaultGuardrails())
                .providers(sanitizeProviders(document.getProviders()))
                .profileDocument(cloneWithoutSecrets(document))
                .build();
    }

    public AgentProfileDocument parseProfile(String profileJson, String traceId) {
        try {
            AgentProfileDocument doc = objectMapper.readValue(profileJson, AgentProfileDocument.class);
            if (doc.getApiVersion() == null) {
                doc.setApiVersion(AgentProfileDocument.API_VERSION);
            }
            return doc;
        } catch (JsonProcessingException ex) {
            throw new ZestLlmException(LlmErrorCode.INTERNAL_ERROR, traceId, "Invalid profile JSON");
        }
    }

    public String toJson(AgentProfileDocument document) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("INVALID_PROFILE", "Profile serialization failed");
        }
    }

    private AgentProfileDocument buildLegacyDocument(LlmModelRouteDO route) {
        AgentProfileDocument doc = new AgentProfileDocument();
        ModelConfig model = new ModelConfig();
        model.setPrimary(route.getPrimaryModel());
        model.setFallback(FallbackModelsParser.parse(route.getFallbackModels(), objectMapper));
        doc.setModel(model);
        GenerationConfig gen = new GenerationConfig();
        gen.setMaxTokens(route.getMaxTokens());
        gen.setTemperature(route.getTemperature() != null ? route.getTemperature().doubleValue() : null);
        gen.setTimeoutMs(route.getTimeoutMs());
        doc.setGeneration(gen);
        return doc;
    }

    private void mergeLegacy(AgentProfileDocument document, LlmPromptVersionDO prompt, LlmModelRouteDO route) {
        if (document.getModel() == null) {
            document.setModel(new ModelConfig());
        }
        if (!StringUtils.hasText(document.getModel().getPrimary())) {
            document.getModel().setPrimary(route.getPrimaryModel());
        }
        if (document.getModel().getFallback() == null || document.getModel().getFallback().isEmpty()) {
            document.getModel().setFallback(FallbackModelsParser.parse(route.getFallbackModels(), objectMapper));
        }
        GenerationConfig gen = generation(document);
        if (gen.getMaxTokens() == null) {
            gen.setMaxTokens(route.getMaxTokens());
        }
        if (gen.getTemperature() == null && route.getTemperature() != null) {
            gen.setTemperature(route.getTemperature().doubleValue());
        }
        if (gen.getTimeoutMs() == null) {
            gen.setTimeoutMs(route.getTimeoutMs());
        }
        if (document.getGuardrails() == null) {
            document.setGuardrails(defaultGuardrails());
        }
        if (prompt.getOutputSchema() != null && document.getExtensions().get("outputSchema") == null) {
            document.getExtensions().put("outputSchema", prompt.getOutputSchema());
        }
    }

    private void enrichTools(AgentProfileDocument document) {
        if (document.getTools() == null || document.getTools().isEmpty()) {
            return;
        }
        for (ToolDefinition tool : document.getTools()) {
            if (tool == null || !"mcp".equalsIgnoreCase(tool.getType())) {
                continue;
            }
            String serverRef = tool.getServerRef();
            if (!StringUtils.hasText(serverRef)) {
                continue;
            }
            mcpServerRepo.findByCode(serverRef).ifPresent(server -> applyMcpServer(tool, server));
        }
    }

    private void applyMcpServer(ToolDefinition tool, LlmMcpServerDO server) {
        tool.getConfig().putIfAbsent("serverUrl", server.getBaseUrl());
        if (StringUtils.hasText(server.getAuthSecretRef())) {
            tool.getConfig().putIfAbsent("authSecretRef", server.getAuthSecretRef());
        }
        if (tool.getName() == null || tool.getName().isBlank()) {
            tool.setName(server.getServerCode());
        }
    }

    private void enrichProviders(AgentProfileDocument document, LlmAgentProfileDO profile) {
        if (document.getProviders() == null) {
            document.setProviders(new LinkedHashMap<>());
        }
        String presetCode = profile != null ? profile.getProviderPresetCode() : null;
        if (StringUtils.hasText(presetCode)) {
            providerPresetRepo.findByCode(presetCode).ifPresent(preset -> mergePreset(document, preset));
        }
        if (StringUtils.hasText(document.getProviderRef())
                && !document.getProviders().containsKey(document.getProviderRef())
                && StringUtils.hasText(presetCode)) {
            providerPresetRepo.findByCode(presetCode).ifPresent(preset ->
                    document.getProviders().put(document.getProviderRef(), parsePresetConfig(preset)));
        }
    }

    private void mergePreset(AgentProfileDocument document, LlmProviderPresetDO preset) {
        ProviderDefinition def = parsePresetConfig(preset);
        String ref = StringUtils.hasText(document.getProviderRef()) ? document.getProviderRef() : preset.getPresetCode();
        document.getProviders().putIfAbsent(ref, def);
        if (!StringUtils.hasText(document.getProviderRef())) {
            document.setProviderRef(ref);
        }
    }

    private ProviderDefinition parsePresetConfig(LlmProviderPresetDO preset) {
        try {
            ProviderDefinition def = objectMapper.readValue(preset.getConfigJson(), ProviderDefinition.class);
            if (def.getType() == null) {
                def.setType(preset.getProviderType());
            }
            if (def.getAuthMode() == null) {
                def.setAuthMode(preset.getAuthMode());
            }
            return def;
        } catch (JsonProcessingException ex) {
            log.warn("Invalid provider preset config {}", preset.getPresetCode());
            ProviderDefinition def = new ProviderDefinition();
            def.setType(preset.getProviderType());
            def.setAuthMode(preset.getAuthMode());
            return def;
        }
    }

    private void applyPolicyJsonOverrides(AgentProfileDocument document, String policyJson) {
        if (!StringUtils.hasText(policyJson)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(policyJson);
            if (node.has("runtimeMode")) {
                document.setRuntimeMode(node.get("runtimeMode").asText());
            }
            if (node.has("providerRef")) {
                document.setProviderRef(node.get("providerRef").asText());
            }
            if (node.has("model")) {
                ModelConfig model = objectMapper.convertValue(node.get("model"), ModelConfig.class);
                if (document.getModel() == null) {
                    document.setModel(model);
                } else {
                    if (StringUtils.hasText(model.getPrimary())) {
                        document.getModel().setPrimary(model.getPrimary());
                    }
                    if (model.getFallback() != null && !model.getFallback().isEmpty()) {
                        document.getModel().setFallback(model.getFallback());
                    }
                }
            }
        } catch (JsonProcessingException ex) {
            log.warn("Invalid policy_json: {}", policyJson);
        }
    }

    private ProviderDefinition resolveActiveProvider(AgentProfileDocument document, LlmAgentProfileDO profile) {
        if (StringUtils.hasText(document.getProviderRef()) && document.getProviders() != null) {
            ProviderDefinition def = document.getProviders().get(document.getProviderRef());
            if (def != null) {
                return def;
            }
        }
        if (profile != null && StringUtils.hasText(profile.getProviderPresetCode())) {
            return providerPresetRepo.findByCode(profile.getProviderPresetCode())
                    .map(this::parsePresetConfig)
                    .orElse(null);
        }
        return null;
    }

    private Map<String, ProviderDefinition> sanitizeProviders(Map<String, ProviderDefinition> providers) {
        if (providers == null) {
            return new LinkedHashMap<>();
        }
        Map<String, ProviderDefinition> copy = new LinkedHashMap<>();
        providers.forEach((key, value) -> {
            if (value != null) {
                copy.put(key, value);
            }
        });
        return copy;
    }

    private AgentProfileDocument cloneWithoutSecrets(AgentProfileDocument source) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(source), AgentProfileDocument.class);
        } catch (JsonProcessingException ex) {
            return source;
        }
    }

    private GenerationConfig generation(AgentProfileDocument document) {
        if (document.getGeneration() == null) {
            document.setGeneration(new GenerationConfig());
        }
        return document.getGeneration();
    }

    private List<String> resolveFallback(AgentProfileDocument document, LlmModelRouteDO route) {
        if (document.getModel() != null && document.getModel().getFallback() != null) {
            return document.getModel().getFallback();
        }
        return FallbackModelsParser.parse(route.getFallbackModels(), objectMapper);
    }

    private GuardrailsConfig defaultGuardrails() {
        GuardrailsConfig guardrails = new GuardrailsConfig();
        guardrails.setBlockOnSchemaMismatch(true);
        return guardrails;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private <T> T firstNonNull(T primary, T fallback) {
        return primary != null ? primary : fallback;
    }
}
