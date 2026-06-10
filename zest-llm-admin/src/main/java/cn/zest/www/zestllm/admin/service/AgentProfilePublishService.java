package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfilePublishResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
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

    @Transactional(rollbackFor = Exception.class)
    public AgentProfilePublishResultVO publish(String taskCode, String version, String operator) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        LlmAgentProfileDO profile = agentProfileRepo.findByTaskIdAndVersion(task.getId(), version)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND",
                        "Profile 版本不存在: " + taskCode + "@" + version));

        String op = operator != null ? operator : "admin";
        agentProfileRepo.unpublishOthers(task.getId(), version);
        agentProfileRepo.publish(task.getId(), version, op);
        auditService.log("PUBLISH", "AGENT_PROFILE", taskCode, Map.of("version", version, "operator", op));
        appRepo.findById(task.getAppId()).ifPresent(app -> {
            policyCacheAdapter.invalidate(app.getAppKey(), task.getCode());
            responseCacheAdapter.invalidate(app.getAppKey(), task.getCode());
        });

        return AgentProfilePublishResultVO.builder()
                .taskCode(taskCode)
                .version(version)
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now())
                .operator(op)
                .build();
    }
}
