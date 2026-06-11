package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.request.ApplyScenarioTemplateRequest;
import cn.zest.www.zestllm.admin.model.request.CreateTaskRequest;
import cn.zest.www.zestllm.admin.model.request.ImportAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileVO;
import cn.zest.www.zestllm.admin.model.vo.ApplyScenarioTemplateResultVO;
import cn.zest.www.zestllm.admin.model.vo.ScenarioTemplateVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioTemplateService {

    private final AgentProfileManageService agentProfileManageService;
    private final AgentProfilePublishService agentProfilePublishService;
    private final AgentProfileResolver agentProfileResolver;
    private final TaskManageService taskManageService;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final ObjectMapper objectMapper;

    private final Map<String, JsonNode> templates = new LinkedHashMap<>();

    @PostConstruct
    void loadTemplates() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:scenario-templates/*.json");
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    JsonNode node = objectMapper.readTree(in);
                    String id = node.path("id").asText(null);
                    if (StringUtils.hasText(id)) {
                        templates.put(id, node);
                    }
                }
            }
            log.info("Loaded {} scenario templates", templates.size());
        } catch (Exception ex) {
            log.warn("Failed to load scenario templates", ex);
        }
    }

    public List<ScenarioTemplateVO> listTemplates() {
        List<ScenarioTemplateVO> result = new ArrayList<>();
        for (JsonNode node : templates.values()) {
            result.add(toSummary(node));
        }
        return result;
    }

    public ScenarioTemplateVO getTemplate(String templateId) {
        JsonNode node = requireTemplate(templateId);
        return toSummary(node);
    }

    public String exportProfileJson(String templateId) {
        JsonNode node = requireTemplate(templateId);
        JsonNode profile = node.get("profileJson");
        if (profile == null || profile.isNull()) {
            throw new BusinessException("TEMPLATE_INVALID", "Template missing profileJson: " + templateId);
        }
        return profile.isTextual() ? profile.asText() : profile.toString();
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplyScenarioTemplateResultVO apply(ApplyScenarioTemplateRequest request) {
        JsonNode node = requireTemplate(request.getTemplateId());
        String taskCode = StringUtils.hasText(request.getTaskCode())
                ? request.getTaskCode()
                : node.path("taskCodeSuggestion").asText("aiJob");
        ensureTask(taskCode, request.getAppKey(), node);

        String version = buildTemplateProfileVersion(request.getTemplateId());
        String profileJson = exportProfileJson(request.getTemplateId());
        AgentProfileVO profile = upsertTemplateProfile(taskCode, version, profileJson, request.isPublish());

        return ApplyScenarioTemplateResultVO.builder()
                .taskCode(taskCode)
                .profileVersion(profile.getVersion())
                .published(request.isPublish())
                .message("Applied template " + request.getTemplateId())
                .build();
    }

    private AgentProfileVO upsertTemplateProfile(String taskCode, String version, String profileJson, boolean publish) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "Task not found: " + taskCode));
        AgentProfileDocument doc = agentProfileResolver.parseProfile(profileJson, null);

        Optional<LlmAgentProfileDO> existing = agentProfileRepo.findByTaskIdAndVersion(task.getId(), version);
        if (existing.isPresent()) {
            LlmAgentProfileDO row = existing.get();
            if ("PUBLISHED".equals(row.getStatus())) {
                String altVersion = version + "-" + Long.toString(System.currentTimeMillis(), 36);
                return importNewProfile(taskCode, altVersion, profileJson, doc, publish);
            }
            UpdateAgentProfileRequest update = new UpdateAgentProfileRequest();
            update.setProfileJson(profileJson);
            update.setProviderPresetCode(doc.getProviderRef());
            update.setRuntimeMode(doc.getRuntimeMode());
            AgentProfileVO updated = agentProfileManageService.updateVersion(taskCode, version, update);
            if (publish) {
                agentProfilePublishService.publish(taskCode, version, "template");
            }
            return updated;
        }
        return importNewProfile(taskCode, version, profileJson, doc, publish);
    }

    private AgentProfileVO importNewProfile(String taskCode, String version, String profileJson,
                                              AgentProfileDocument doc, boolean publish) {
        ImportAgentProfileRequest importReq = new ImportAgentProfileRequest();
        importReq.setTaskCode(taskCode);
        importReq.setVersion(version);
        importReq.setProfileJson(profileJson);
        importReq.setPublish(publish);
        return agentProfileManageService.importProfile(importReq);
    }

    private void ensureTask(String taskCode, String appKey, JsonNode node) {
        Optional<LlmAiTaskDefDO> existing = taskDefRepo.findByCode(taskCode);
        if (existing.isPresent()) {
            return;
        }
        CreateTaskRequest create = new CreateTaskRequest();
        create.setAppKey(appKey);
        create.setCode(taskCode);
        create.setName(node.path("taskName").asText(taskCode));
        create.setDescription(node.path("description").asText(""));
        taskManageService.create(create);
    }

    /** Stable draft version per template — re-apply updates the same DRAFT row. */
    private String buildTemplateProfileVersion(String templateId) {
        String slug = templateId.replace("-", "");
        if (slug.length() > 20) {
            slug = slug.substring(0, 20);
        }
        return "v-tpl-" + slug;
    }

    private JsonNode requireTemplate(String templateId) {
        JsonNode node = templates.get(templateId);
        if (node == null) {
            throw new BusinessException("TEMPLATE_NOT_FOUND", "Scenario template not found: " + templateId);
        }
        return node;
    }

    private ScenarioTemplateVO toSummary(JsonNode node) {
        return ScenarioTemplateVO.builder()
                .id(node.path("id").asText())
                .name(node.path("name").asText())
                .description(node.path("description").asText())
                .recommendedTier(node.path("recommendedTier").asText("small"))
                .taskCodeSuggestion(node.path("taskCodeSuggestion").asText())
                .taskName(node.path("taskName").asText())
                .runtimeMode(node.path("runtimeMode").asText("agent"))
                .requiresMcp(node.path("requiresMcp").asBoolean(false))
                .requiresKnowledge(node.path("requiresKnowledge").asBoolean(false))
                .build();
    }
}
