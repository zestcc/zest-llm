package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfilePublishResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentProfilePublishService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmAppRepo appRepo;
    private final PolicyCacheAdapter policyCacheAdapter;
    private final ResponseCacheAdapter responseCacheAdapter;
    private final AuditService auditService;
    private final AgentProfileResolver agentProfileResolver;
    private final ProfileExtensionsValidator profileExtensionsValidator;
    private final ZestEvalLearningPipelineAdapter learningPipelineAdapter;
    private final AgentProfileProbeService agentProfileProbeService;
    private final IntegrationWebhookService integrationWebhookService;

    @Transactional(rollbackFor = Exception.class)
    public AgentProfilePublishResultVO publish(String taskCode, String version, String operator) {
        String op = operator != null ? operator : "admin";
        try {
            AgentProfilePublishResultVO result = doPublish(taskCode, version, op);
            integrationWebhookService.notifyPublishResult(taskCode, version, true, "published", op);
            return result;
        } catch (BusinessException ex) {
            integrationWebhookService.notifyPublishResult(taskCode, version, false, ex.getMessage(), op);
            throw ex;
        }
    }

    private AgentProfilePublishResultVO doPublish(String taskCode, String version, String operator) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        LlmAgentProfileDO profile = agentProfileRepo.findByTaskIdAndVersion(task.getId(), version)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND",
                        "Profile 版本不存在: " + taskCode + "@" + version));

        AgentProfileDocument document = agentProfileResolver.parseProfile(profile.getProfileJson(), null);
        profileExtensionsValidator.validate(document);

        LearningLoopConfig loop = ProfileExtensions.learningLoop(document).orElse(null);
        if (loop != null && loop.isEnabled()) {
            LearningCycleResult cycle = learningPipelineAdapter.validateForPublish(taskCode, version, document);
            if (loop.isProbeBeforePublish() && !cycle.isProbePassed()) {
                throw new BusinessException("PROBE_FAILED",
                        "发布门禁：探测未通过 · " + cycle.getMessage(), 409);
            }
            if (cycle.getPassRate() < loop.getMinPassRate()) {
                throw new BusinessException("EVAL_BELOW_THRESHOLD",
                        "发布门禁：Eval 通过率 " + String.format("%.2f", cycle.getPassRate() * 100)
                                + "% 低于阈值 " + String.format("%.0f", loop.getMinPassRate() * 100) + "%", 409);
            }
        } else {
            AgentProfileProbeResultVO probe = agentProfileProbeService.probeVersion(
                    taskCode, version, new AgentProfileProbeRequest());
            if (!probe.isReady()) {
                throw new BusinessException("PROBE_FAILED",
                        "发布门禁：探测未通过 · " + probe.getOverallStatus(), 409);
            }
        }

        String op = operator != null ? operator : "admin";
        agentProfileRepo.unpublishOthers(task.getId(), version);
        agentProfileRepo.publish(task.getId(), version, op);
        auditService.log("PUBLISH", "AGENT_PROFILE", taskCode, Map.of("version", version, "operator", op));
        appRepo.findById(task.getAppId()).ifPresent(app -> invalidateCache(app, task.getCode()));

        return AgentProfilePublishResultVO.builder()
                .taskCode(taskCode)
                .version(version)
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now())
                .operator(op)
                .build();
    }

    private void invalidateCache(LlmAppDO app, String taskCode) {
        policyCacheAdapter.invalidate(app.getAppKey(), taskCode);
        responseCacheAdapter.invalidate(app.getAppKey(), taskCode);
    }
}
