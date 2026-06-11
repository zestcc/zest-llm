package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.KnowledgeRefConfig;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;
import cn.zest.www.zestllm.spi.profile.RuntimeBackendConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class ProfileExtensionsValidator {

    private static final Set<String> RUNTIME_TYPES = Set.of("native", "dify", "fastgpt", "custom");
    private static final Set<String> RUNTIME_MODES = Set.of("invoke", "agent", "external", "hybrid");
    private static final Set<String> INJECT_MODES = Set.of("system_prefix", "external", "none");

    public void validate(AgentProfileDocument document) {
        if (document == null) {
            throw new BusinessException("INVALID_PROFILE", "Profile document is null");
        }
        String runtimeMode = document.getRuntimeMode();
        if (StringUtils.hasText(runtimeMode) && !RUNTIME_MODES.contains(runtimeMode)) {
            throw new BusinessException("INVALID_PROFILE", "Unsupported runtimeMode: " + runtimeMode);
        }
        ProfileExtensions.runtimeBackend(document).ifPresent(this::validateRuntimeBackend);
        ProfileExtensions.knowledge(document).ifPresent(this::validateKnowledge);
        ProfileExtensions.learningLoop(document).ifPresent(this::validateLearningLoop);

        if ("external".equalsIgnoreCase(runtimeMode)) {
            RuntimeBackendConfig backend = ProfileExtensions.runtimeBackend(document)
                    .orElseThrow(() -> new BusinessException("INVALID_PROFILE", "external runtime requires extensions.runtimeBackend"));
            if ("native".equalsIgnoreCase(backend.getType())) {
                throw new BusinessException("INVALID_PROFILE", "external runtime cannot use native backend type");
            }
        }
        if ("hybrid".equalsIgnoreCase(runtimeMode)) {
            KnowledgeRefConfig knowledge = ProfileExtensions.knowledge(document)
                    .orElseThrow(() -> new BusinessException("INVALID_PROFILE", "hybrid runtime requires extensions.knowledge"));
            if (!knowledge.isEnabled()) {
                throw new BusinessException("INVALID_PROFILE", "hybrid runtime requires knowledge.enabled=true");
            }
        }
    }

    private void validateRuntimeBackend(RuntimeBackendConfig config) {
        if (!StringUtils.hasText(config.getType()) || !RUNTIME_TYPES.contains(config.getType())) {
            throw new BusinessException("INVALID_PROFILE", "Invalid runtimeBackend.type: " + config.getType());
        }
        if (!"native".equalsIgnoreCase(config.getType()) && !StringUtils.hasText(config.getBaseUrl())) {
            throw new BusinessException("INVALID_PROFILE", "runtimeBackend.baseUrl is required");
        }
        if (!"native".equalsIgnoreCase(config.getType()) && !StringUtils.hasText(config.getExternalAppId())) {
            throw new BusinessException("INVALID_PROFILE", "runtimeBackend.externalAppId required for type=" + config.getType());
        }
    }

    private void validateKnowledge(KnowledgeRefConfig config) {
        if (!config.isEnabled()) {
            return;
        }
        if (config.getDatasetIds() == null || config.getDatasetIds().isEmpty()) {
            throw new BusinessException("INVALID_PROFILE", "knowledge.datasetIds required when enabled");
        }
        if (StringUtils.hasText(config.getInjectMode()) && !INJECT_MODES.contains(config.getInjectMode())) {
            throw new BusinessException("INVALID_PROFILE", "Invalid knowledge.injectMode: " + config.getInjectMode());
        }
    }

    private void validateLearningLoop(LearningLoopConfig config) {
        if (!config.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(config.getEvalDatasetRef())) {
            throw new BusinessException("INVALID_PROFILE", "learningLoop.evalDatasetRef required when enabled");
        }
        if (config.getMinPassRate() < 0 || config.getMinPassRate() > 1) {
            throw new BusinessException("INVALID_PROFILE", "learningLoop.minPassRate must be 0..1");
        }
    }
}
