package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.PublishPreviewVO;
import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentProfilePublishPreviewService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final AgentProfileResolver agentProfileResolver;
    private final ProfileExtensionsValidator profileExtensionsValidator;
    private final ZestEvalLearningPipelineAdapter learningPipelineAdapter;
    private final AgentProfileProbeService agentProfileProbeService;

    public PublishPreviewVO preview(String taskCode, String version) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        LlmAgentProfileDO profile = agentProfileRepo.findByTaskIdAndVersion(task.getId(), version)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile 版本不存在"));
        AgentProfileDocument document = agentProfileResolver.parseProfile(profile.getProfileJson(), null);
        profileExtensionsValidator.validate(document);

        LearningLoopConfig loop = ProfileExtensions.learningLoop(document).orElse(null);
        if (loop == null || !loop.isEnabled()) {
            AgentProfileProbeResultVO probe = agentProfileProbeService.probeVersion(
                    taskCode, version, new AgentProfileProbeRequest());
            return PublishPreviewVO.builder()
                    .taskCode(taskCode)
                    .version(version)
                    .learningLoopEnabled(false)
                    .probePassed(probe.isReady())
                    .publishAllowed(probe.isReady())
                    .passRate(1.0)
                    .message(probe.isReady() ? "无 Learning 门禁，Probe 通过可发布" : "Probe 未通过")
                    .build();
        }

        LearningCycleResult cycle = learningPipelineAdapter.validateForPublish(taskCode, version, document);
        return PublishPreviewVO.builder()
                .taskCode(taskCode)
                .version(version)
                .learningLoopEnabled(true)
                .probePassed(cycle.isProbePassed())
                .publishAllowed(cycle.isPublishAllowed())
                .passRate(cycle.getPassRate())
                .totalCases(cycle.getTotalCases())
                .passedCases(cycle.getPassedCases())
                .message(cycle.getMessage())
                .failedCaseCodes(cycle.getFailedCaseCodes())
                .build();
    }
}
