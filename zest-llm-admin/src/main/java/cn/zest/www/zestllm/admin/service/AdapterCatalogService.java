package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.vo.AdapterCatalogDetailVO;
import cn.zest.www.zestllm.admin.model.vo.AdapterCatalogItemVO;
import cn.zest.www.zestllm.admin.model.vo.AdapterCatalogPageVO;
import cn.zest.www.zestllm.admin.model.vo.AdapterHealthVO;
import cn.zest.www.zestllm.admin.model.vo.AdapterIntegrationStepVO;
import cn.zest.www.zestllm.admin.plugin.AdapterCatalogDefinitions;
import cn.zest.www.zestllm.admin.plugin.AdapterCatalogEntry;
import cn.zest.www.zestllm.admin.plugin.AdapterEnablementChecker;
import cn.zest.www.zestllm.admin.plugin.AdapterIntegrationStep;
import cn.zest.www.zestllm.admin.plugin.AdapterLoadStatus;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.config.LlmPluginProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdapterCatalogService {

    private final AdapterEnablementChecker enablementChecker;
    private final AdapterConfigService adapterConfigService;
    private final AdapterHealthService adapterHealthService;
    private final LlmAdapterProperties adapterProperties;
    private final Environment environment;
    private final ExternalAdapterRegistry externalAdapterRegistry;
    private final LlmPluginProperties pluginProperties;

    public AdapterCatalogPageVO catalog(String spiTypeFilter) {
        List<AdapterCatalogItemVO> items = new ArrayList<>();
        for (AdapterCatalogEntry entry : AdapterCatalogDefinitions.all()) {
            if (StringUtils.hasText(spiTypeFilter) && !entry.getSpiType().equals(spiTypeFilter)) {
                continue;
            }
            items.add(toItem(entry));
        }
        items.sort(Comparator.comparing(AdapterCatalogItemVO::getSpiType)
                .thenComparing(AdapterCatalogItemVO::getPluginId));

        Map<String, Object> summary = new HashMap<>(8);
        summary.put("total", items.size());
        summary.put("installed", items.stream().filter(AdapterCatalogItemVO::isInstalled).count());
        summary.put("active", items.stream().filter(AdapterCatalogItemVO::isActive).count());
        summary.put("healthy", items.stream().filter(AdapterCatalogItemVO::isHealthUp).count());

        Map<String, String> activeDefaults = new LinkedHashMap<>();
        for (String spi : spiTypes()) {
            activeDefaults.put(spi, enablementChecker.resolveActivePluginId(spi));
        }

        return AdapterCatalogPageVO.builder()
                .profile(String.join(",", environment.getActiveProfiles()))
                .summary(summary)
                .plugins(items)
                .activeDefaults(activeDefaults)
                .runtimeOverrides(adapterConfigService.listOverrides())
                .externalDir(pluginProperties.getExternalDir())
                .externalPlugins(externalAdapterRegistry.listViews())
                .build();
    }

    public AdapterCatalogDetailVO detail(String catalogKey) {
        AdapterCatalogEntry entry = Optional.ofNullable(AdapterCatalogDefinitions.findByKey(catalogKey))
                .orElseThrow(() -> new BusinessException("PLUGIN_NOT_FOUND", "插件不存在: " + catalogKey));
        AdapterCatalogItemVO item = toItem(entry);
        String configured = enablementChecker.resolveActivePluginId(entry.getSpiType());
        String pending = adapterConfigService.pendingPluginId(entry.getSpiType());
        boolean restartRequired = pending != null && !pending.equals(readYamlValue(entry.getSpiType()));

        return AdapterCatalogDetailVO.builder()
                .catalogKey(entry.catalogKey())
                .pluginId(entry.getPluginId())
                .pluginName(entry.getPluginName())
                .spiType(entry.getSpiType())
                .description(entry.getDescription())
                .vendor(entry.getVendor())
                .version(entry.getVersion())
                .configProperty(entry.getConfigProperty())
                .configExample(entry.getConfigExample())
                .mavenArtifact(entry.getMavenArtifact())
                .installed(isInstalled(entry))
                .builtIn(entry.isBuiltIn())
                .active(item.isActive())
                .loadStatus(item.getLoadStatus())
                .configuredValue(configured)
                .pendingValue(pending)
                .restartRequired(restartRequired)
                .healthUp(item.isHealthUp())
                .healthMessage(item.getHealthMessage())
                .prerequisites(entry.getPrerequisites())
                .relatedTemplates(entry.getRelatedTemplates())
                .integrationSteps(toSteps(entry.getIntegrationSteps()))
                .runtimeOverrides(adapterConfigService.listOverrides())
                .build();
    }

    public AdapterCatalogDetailVO healthCheck(String catalogKey) {
        AdapterCatalogDetailVO detail = detail(catalogKey);
        if (!detail.isActive()) {
            return detail.toBuilder()
                    .healthUp(false)
                    .healthMessage("插件未激活，健康探测跳过")
                    .build();
        }
        AdapterHealthVO health = resolveHealth(detail.getSpiType());
        return detail.toBuilder()
                .healthUp(health.isUp())
                .healthMessage(health.getMessage())
                .build();
    }

    private AdapterCatalogItemVO toItem(AdapterCatalogEntry entry) {
        boolean active = enablementChecker.isActive(entry.getSpiType(), entry.getPluginId());
        AdapterLoadStatus loadStatus = resolveLoadStatus(entry, active);
        AdapterHealthVO health = active ? resolveHealth(entry.getSpiType()) : inactiveHealth(entry);

        return AdapterCatalogItemVO.builder()
                .catalogKey(entry.catalogKey())
                .pluginId(entry.getPluginId())
                .pluginName(entry.getPluginName())
                .spiType(entry.getSpiType())
                .description(entry.getDescription())
                .vendor(entry.getVendor())
                .version(entry.getVersion())
                .loadStatus(loadStatus.name())
                .active(active)
                .installed(isInstalled(entry))
                .builtIn(entry.isBuiltIn())
                .external(isExternal(entry))
                .healthUp(health.isUp())
                .healthMessage(health.getMessage())
                .build();
    }

    private AdapterLoadStatus resolveLoadStatus(AdapterCatalogEntry entry, boolean active) {
        if (!isInstalled(entry)) {
            return AdapterLoadStatus.NOT_INSTALLED;
        }
        return active ? AdapterLoadStatus.LOADED : AdapterLoadStatus.DISABLED;
    }

    private boolean isInstalled(AdapterCatalogEntry entry) {
        return entry.isInstalled() || externalAdapterRegistry.isRegistered(entry.getSpiType(), entry.getPluginId());
    }

    private boolean isExternal(AdapterCatalogEntry entry) {
        return externalAdapterRegistry.isRegistered(entry.getSpiType(), entry.getPluginId());
    }

    private AdapterHealthVO resolveHealth(String spiType) {
        return adapterHealthService.listAll().stream()
                .filter(item -> spiType.equals(item.getKind()))
                .findFirst()
                .orElse(AdapterHealthVO.builder()
                        .kind(spiType)
                        .adapterId(enablementChecker.resolveActivePluginId(spiType))
                        .up(true)
                        .message("ok")
                        .build());
    }

    private AdapterHealthVO inactiveHealth(AdapterCatalogEntry entry) {
        return AdapterHealthVO.builder()
                .kind(entry.getSpiType())
                .adapterId(entry.getPluginId())
                .up(false)
                .message(entry.isInstalled() || isExternal(entry) ? "未激活" : "未安装")
                .build();
    }

    private List<AdapterIntegrationStepVO> toSteps(List<AdapterIntegrationStep> steps) {
        if (steps == null) {
            return List.of();
        }
        return steps.stream()
                .sorted(Comparator.comparingInt(AdapterIntegrationStep::getOrder))
                .map(step -> AdapterIntegrationStepVO.builder()
                        .stepId(step.getStepId())
                        .order(step.getOrder())
                        .title(step.getTitle())
                        .description(step.getDescription())
                        .actionType(step.getActionType())
                        .actionLabel(step.getActionLabel())
                        .actionTarget(step.getActionTarget())
                        .commandExample(step.getCommandExample())
                        .docUrl(step.getDocUrl())
                        .required(step.isRequired())
                        .build())
                .toList();
    }

    private String readYamlValue(String spiType) {
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
            case "mcp-tool" -> "http-mcp";
            default -> "";
        };
    }

    private List<String> spiTypes() {
        return List.of(
                "model-gateway", "observability", "agent-runtime", "knowledge-retrieval",
                "learning-pipeline", "policy-cache", "response-cache", "quota", "audit",
                "prompt-renderer", "output-schema-validator", "report-channel",
                "content-moderation", "alert-webhook", "mcp-tool"
        );
    }
}
