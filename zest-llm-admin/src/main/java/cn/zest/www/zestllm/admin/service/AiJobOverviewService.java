package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileProbeDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.vo.AiJobOverviewVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileProbeRepo;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiJobOverviewService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAppRepo appRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmAgentProfileProbeRepo probeRepo;
    private final LlmExecutionRepo executionRepo;

    public List<AiJobOverviewVO> listOverview() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<AiJobOverviewVO> result = new ArrayList<>();
        for (LlmAiTaskDefDO task : taskDefRepo.findAll()) {
            String appKey = appRepo.findById(task.getAppId()).map(LlmAppDO::getAppKey).orElse("-");
            Optional<LlmAgentProfileDO> published = agentProfileRepo.findPublishedByTaskId(task.getId());
            Optional<LlmAgentProfileProbeDO> probe = probeRepo.findLatestByTaskId(task.getId());
            long total = executionRepo.countByTaskCodeSince(task.getCode(), since);
            long failed = executionRepo.countByTaskCodeAndStatusSince(task.getCode(), "FAILED", since);
            result.add(AiJobOverviewVO.builder()
                    .code(task.getCode())
                    .name(task.getName())
                    .appKey(appKey)
                    .status(task.getStatus())
                    .publishedVersion(published.map(LlmAgentProfileDO::getVersion).orElse(null))
                    .probeStatus(probe.map(LlmAgentProfileProbeDO::getOverallStatus).orElse("UNKNOWN"))
                    .lastProbeAt(probe.map(LlmAgentProfileProbeDO::getCreatedAt).orElse(null))
                    .executionsLast7d(total)
                    .failedLast7d(failed)
                    .build());
        }
        return result;
    }
}
