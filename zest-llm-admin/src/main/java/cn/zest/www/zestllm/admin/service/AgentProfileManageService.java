package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.request.CreateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.ImportAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfilePublishResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentProfileManageService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmAppRepo appRepo;
    private final AgentProfileResolver agentProfileResolver;
    private final AgentProfilePublishService agentProfilePublishService;
    private final AuditService auditService;
    private final PolicyCacheAdapter policyCacheAdapter;
    private final ResponseCacheAdapter responseCacheAdapter;
    private final ProfileExtensionsValidator profileExtensionsValidator;

    public List<AgentProfileVO> listVersions(String taskCode) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        return agentProfileRepo.findByTaskId(task.getId()).stream()
                .map(row -> toVO(taskCode, row))
                .toList();
    }

    public AgentProfileVO getPublished(String taskCode) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        return agentProfileRepo.findPublishedByTaskId(task.getId())
                .map(row -> toVO(taskCode, row))
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "No published profile: " + taskCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentProfileVO createVersion(String taskCode, CreateAgentProfileRequest request) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        validateProfileJson(request.getProfileJson());
        if (agentProfileRepo.findByTaskIdAndVersion(task.getId(), request.getVersion()).isPresent()) {
            throw new BusinessException("PROFILE_EXISTS", "Version exists: " + request.getVersion());
        }
        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setTaskId(task.getId());
        profile.setVersion(request.getVersion());
        profile.setProfileJson(normalizeJson(request.getProfileJson()));
        profile.setProviderPresetCode(request.getProviderPresetCode());
        profile.setRuntimeMode(StringUtils.hasText(request.getRuntimeMode()) ? request.getRuntimeMode() : "invoke");
        profile.setStatus("DRAFT");
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        agentProfileRepo.insert(profile);
        auditService.log("CREATE", "AGENT_PROFILE", taskCode, Map.of("version", request.getVersion()));
        return toVO(taskCode, profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentProfileVO updateVersion(String taskCode, String version, UpdateAgentProfileRequest request) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        LlmAgentProfileDO profile = requireProfile(task.getId(), version);
        if ("PUBLISHED".equals(profile.getStatus())) {
            throw new BusinessException("PROFILE_PUBLISHED", "Create a new draft to edit published profile");
        }
        validateProfileJson(request.getProfileJson());
        profile.setProfileJson(normalizeJson(request.getProfileJson()));
        profile.setProviderPresetCode(request.getProviderPresetCode());
        if (StringUtils.hasText(request.getRuntimeMode())) {
            profile.setRuntimeMode(request.getRuntimeMode());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        agentProfileRepo.update(profile);
        auditService.log("UPDATE", "AGENT_PROFILE", taskCode, Map.of("version", version));
        return toVO(taskCode, profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentProfilePublishResultVO publish(String taskCode, String version, String operator) {
        return agentProfilePublishService.publish(taskCode, version, operator);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentProfilePublishResultVO rollback(String taskCode, String version) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        requireProfile(task.getId(), version);
        AgentProfilePublishResultVO result = agentProfilePublishService.publish(taskCode, version, null);
        auditService.log("ROLLBACK", "AGENT_PROFILE", taskCode, Map.of("version", version));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentProfileVO importProfile(ImportAgentProfileRequest request) {
        if (!StringUtils.hasText(request.getTaskCode())) {
            throw new BusinessException("TASK_REQUIRED", "taskCode is required");
        }
        validateProfileJson(request.getProfileJson());
        AgentProfileDocument doc = agentProfileResolver.parseProfile(request.getProfileJson(), null);
        String version = StringUtils.hasText(request.getVersion()) ? request.getVersion() : "v" + System.currentTimeMillis();
        CreateAgentProfileRequest create = new CreateAgentProfileRequest();
        create.setVersion(version);
        create.setProfileJson(request.getProfileJson());
        create.setProviderPresetCode(doc.getProviderRef());
        create.setRuntimeMode(doc.getRuntimeMode());
        AgentProfileVO created = createVersion(request.getTaskCode(), create);
        if (request.isPublish()) {
            agentProfilePublishService.publish(request.getTaskCode(), version, "import");
        }
        return created;
    }

    public String exportProfile(String taskCode, String version) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        LlmAgentProfileDO profile = requireProfile(task.getId(), version);
        return profile.getProfileJson();
    }

    @Transactional(rollbackFor = Exception.class)
    public void activateProvider(String taskCode, String providerRef) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        LlmAgentProfileDO profile = agentProfileRepo.findPublishedByTaskId(task.getId())
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "No published profile"));
        AgentProfileDocument doc = agentProfileResolver.parseProfile(profile.getProfileJson(), null);
        doc.setProviderRef(providerRef);
        profile.setProfileJson(agentProfileResolver.toJson(doc));
        profile.setProviderPresetCode(providerRef);
        profile.setUpdatedAt(LocalDateTime.now());
        agentProfileRepo.update(profile);
        invalidateCache(task);
        auditService.log("ACTIVATE_PROVIDER", "AGENT_PROFILE", taskCode, Map.of("providerRef", providerRef));
    }

    private void validateProfileJson(String profileJson) {
        AgentProfileDocument doc = agentProfileResolver.parseProfile(profileJson, null);
        profileExtensionsValidator.validate(doc);
    }

    private String normalizeJson(String profileJson) {
        AgentProfileDocument doc = agentProfileResolver.parseProfile(profileJson, null);
        return agentProfileResolver.toJson(doc);
    }

    private LlmAiTaskDefDO requireTask(String taskCode) {
        return taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "Task not found: " + taskCode));
    }

    private LlmAgentProfileDO requireProfile(Long taskId, String version) {
        return agentProfileRepo.findByTaskIdAndVersion(taskId, version)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile version not found: " + version));
    }

    private void invalidateCache(LlmAiTaskDefDO task) {
        appRepo.findById(task.getAppId()).ifPresent(app -> {
            policyCacheAdapter.invalidate(app.getAppKey(), task.getCode());
            responseCacheAdapter.invalidate(app.getAppKey(), task.getCode());
        });
    }

    private AgentProfileVO toVO(String taskCode, LlmAgentProfileDO profile) {
        return AgentProfileVO.builder()
                .id(profile.getId())
                .taskCode(taskCode)
                .version(profile.getVersion())
                .profileJson(profile.getProfileJson())
                .providerPresetCode(profile.getProviderPresetCode())
                .runtimeMode(profile.getRuntimeMode())
                .status(profile.getStatus())
                .publishedAt(profile.getPublishedAt())
                .createdBy(profile.getCreatedBy())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
