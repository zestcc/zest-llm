package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.dto.PublishPromptCommand;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.vo.PromptPublishResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptPublishService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmPromptVersionRepo promptVersionRepo;
    private final LlmAppRepo appRepo;
    private final PolicyCacheAdapter policyCacheAdapter;
    private final AuditService auditService;

    @Transactional(rollbackFor = Exception.class)
    public PromptPublishResultVO publish(String taskCode, String version, String operator) {
        PublishPromptCommand command = new PublishPromptCommand();
        command.setTaskCode(taskCode);
        command.setVersion(version);
        command.setOperator(operator);
        return publish(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptPublishResultVO publish(PublishPromptCommand command) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(command.getTaskCode())
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + command.getTaskCode()));
        LlmPromptVersionDO prompt = promptVersionRepo.findByTaskIdAndVersion(task.getId(), command.getVersion())
                .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND",
                        "Prompt 版本不存在: " + command.getTaskCode() + "@" + command.getVersion()));

        String operator = command.getOperator() != null ? command.getOperator() : "admin";
        promptVersionRepo.unpublishOthers(task.getId(), command.getVersion());
        promptVersionRepo.publish(task.getId(), command.getVersion(), operator);
        auditService.log("PUBLISH", "PROMPT", task.getCode(),
                Map.of("version", command.getVersion(), "operator", operator));
        invalidatePolicyCache(task);

        return PromptPublishResultVO.builder()
                .taskCode(task.getCode())
                .version(command.getVersion())
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now())
                .operator(operator)
                .build();
    }

    private void invalidatePolicyCache(LlmAiTaskDefDO task) {
        appRepo.findById(task.getAppId()).ifPresent(app ->
                policyCacheAdapter.invalidate(app.getAppKey(), task.getCode()));
    }
}
