package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmLearningCycleRunDO;
import cn.zest.www.zestllm.admin.model.vo.LearningCycleRunVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmLearningCycleRunRepo;
import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.LearningCycleRequest;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningManageService {

    private final ZestEvalLearningPipelineAdapter learningPipelineAdapter;
    private final AgentProfileResolver agentProfileResolver;
    private final LlmLearningCycleRunRepo learningCycleRunRepo;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;

    public List<EvalCaseSuggestion> suggestCases(TraceSampleQuery query) {
        return learningPipelineAdapter.suggestCasesFromTraces(query);
    }

    public LearningCycleResult runCycle(String taskCode, String profileVersion, boolean dryRun) {
        AgentProfileDocument document = loadProfileDocument(taskCode, profileVersion);
        LearningLoopConfig loop = ProfileExtensions.learningLoop(document).orElseGet(LearningLoopConfig::new);
        return learningPipelineAdapter.runCycle(LearningCycleRequest.builder()
                .taskCode(taskCode)
                .profileVersion(profileVersion)
                .learningLoop(loop)
                .dryRun(dryRun)
                .build());
    }

    public Page<LearningCycleRunVO> pageCycles(String taskCode, int page, int size) {
        Page<LlmLearningCycleRunDO> raw = learningCycleRunRepo.pageByTask(taskCode, page, size);
        Page<LearningCycleRunVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(this::toVo).toList());
        return result;
    }

    /**
     * 对已发布且 extensions.learningLoop.enabled 的作业执行闭环（供定时任务调用）。
     */
    public int runScheduledCycles(boolean dryRun) {
        List<LlmAgentProfileDO> profiles = agentProfileRepo.findAllPublished();
        int count = 0;
        for (LlmAgentProfileDO profile : profiles) {
            LlmAiTaskDefDO task = taskDefRepo.findById(profile.getTaskId()).orElse(null);
            if (task == null) {
                continue;
            }
            try {
                AgentProfileDocument document = agentProfileResolver.parseProfile(profile.getProfileJson(), null);
                LearningLoopConfig loop = ProfileExtensions.learningLoop(document).orElse(null);
                if (loop == null || !loop.isEnabled()) {
                    continue;
                }
                runCycle(task.getCode(), profile.getVersion(), dryRun);
                count++;
            } catch (Exception ex) {
                log.warn("Scheduled learning cycle failed taskCode={} version={}",
                        task.getCode(), profile.getVersion(), ex);
            }
        }
        return count;
    }

    private AgentProfileDocument loadProfileDocument(String taskCode, String profileVersion) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        LlmAgentProfileDO profile = agentProfileRepo.findByTaskIdAndVersion(task.getId(), profileVersion)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile 版本不存在"));
        return agentProfileResolver.parseProfile(profile.getProfileJson(), null);
    }

    private LearningCycleRunVO toVo(LlmLearningCycleRunDO row) {
        return LearningCycleRunVO.builder()
                .runCode(row.getRunCode())
                .taskCode(row.getTaskCode())
                .profileVersion(row.getProfileVersion())
                .passRate(row.getPassRate())
                .probePassed(row.getProbePassed())
                .publishAllowed(row.getPublishAllowed())
                .status(row.getStatus())
                .message(row.getMessage())
                .startedAt(row.getStartedAt())
                .finishedAt(row.getFinishedAt())
                .build();
    }
}
