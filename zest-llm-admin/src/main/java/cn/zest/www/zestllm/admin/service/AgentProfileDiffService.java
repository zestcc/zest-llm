package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.vo.DiffEntryVO;
import cn.zest.www.zestllm.admin.model.vo.VersionDiffVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.GenerationConfig;
import cn.zest.www.zestllm.spi.profile.GuardrailsConfig;
import cn.zest.www.zestllm.spi.profile.ModelConfig;
import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AgentProfileDiffService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final AgentProfileResolver agentProfileResolver;

    public VersionDiffVO diff(String taskCode, String fromVersion, String toVersion) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "Task not found: " + taskCode));
        LlmAgentProfileDO from = agentProfileRepo.findByTaskIdAndVersion(task.getId(), fromVersion)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile not found: " + fromVersion));
        LlmAgentProfileDO to = agentProfileRepo.findByTaskIdAndVersion(task.getId(), toVersion)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile not found: " + toVersion));

        AgentProfileDocument fromDoc = agentProfileResolver.parseProfile(from.getProfileJson(), null);
        AgentProfileDocument toDoc = agentProfileResolver.parseProfile(to.getProfileJson(), null);

        List<DiffEntryVO> changes = new ArrayList<>();
        addChange(changes, "runtimeMode", fromDoc.getRuntimeMode(), toDoc.getRuntimeMode());
        addChange(changes, "providerRef", fromDoc.getProviderRef(), toDoc.getProviderRef());
        addChange(changes, "toolCallMode", fromDoc.getToolCallMode(), toDoc.getToolCallMode());
        addChange(changes, "primaryModel", modelPrimary(fromDoc.getModel()), modelPrimary(toDoc.getModel()));
        addChange(changes, "maxTokens", genField(fromDoc.getGeneration(), GenerationConfig::getMaxTokens),
                genField(toDoc.getGeneration(), GenerationConfig::getMaxTokens));
        addChange(changes, "temperature", genField(fromDoc.getGeneration(), GenerationConfig::getTemperature),
                genField(toDoc.getGeneration(), GenerationConfig::getTemperature));
        addChange(changes, "maxToolSteps", genField(fromDoc.getGeneration(), GenerationConfig::getMaxToolSteps),
                genField(toDoc.getGeneration(), GenerationConfig::getMaxToolSteps));
        addChange(changes, "tools", toolNames(fromDoc), toolNames(toDoc));
        addChange(changes, "fallbackModels", fallbackModels(fromDoc.getModel()), fallbackModels(toDoc.getModel()));
        addChange(changes, "piiRedact", guardrailField(fromDoc, "piiRedact"), guardrailField(toDoc, "piiRedact"));
        addChange(changes, "providerPresetCode", from.getProviderPresetCode(), to.getProviderPresetCode());

        return VersionDiffVO.builder()
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .changes(changes)
                .build();
    }

    private String modelPrimary(ModelConfig model) {
        return model != null ? model.getPrimary() : null;
    }

    private <T> String genField(GenerationConfig gen, java.util.function.Function<GenerationConfig, T> fn) {
        if (gen == null) {
            return null;
        }
        T value = fn.apply(gen);
        return value != null ? String.valueOf(value) : null;
    }

    private String toolsCount(AgentProfileDocument doc) {
        return doc.getTools() != null ? String.valueOf(doc.getTools().size()) : "0";
    }

    private String toolNames(AgentProfileDocument doc) {
        if (doc.getTools() == null || doc.getTools().isEmpty()) {
            return null;
        }
        return doc.getTools().stream()
                .map(ToolDefinition::getName)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }

    private String fallbackModels(ModelConfig model) {
        if (model == null || model.getFallback() == null || model.getFallback().isEmpty()) {
            return null;
        }
        return String.join(", ", model.getFallback());
    }

    private String guardrailField(AgentProfileDocument doc, String field) {
        GuardrailsConfig guardrails = doc.getGuardrails();
        if (guardrails == null) {
            return null;
        }
        if ("piiRedact".equals(field)) {
            return guardrails.getPiiRedact() != null ? String.valueOf(guardrails.getPiiRedact()) : null;
        }
        return null;
    }

    private void addChange(List<DiffEntryVO> changes, String field, String before, String after) {
        if (Objects.equals(before, after)) {
            return;
        }
        changes.add(DiffEntryVO.builder()
                .field(field)
                .changeType(before == null ? "ADDED" : after == null ? "REMOVED" : "MODIFIED")
                .before(before)
                .after(after)
                .build());
    }
}
