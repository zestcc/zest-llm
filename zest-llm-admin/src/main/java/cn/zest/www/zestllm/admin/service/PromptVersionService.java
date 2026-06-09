package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.request.CreatePromptVersionRequest;
import cn.zest.www.zestllm.admin.model.vo.PromptPublishResultVO;
import cn.zest.www.zestllm.admin.model.vo.PromptVersionVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptVersionService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmPromptVersionRepo promptVersionRepo;
    private final PromptPublishService promptPublishService;
    private final AuditService auditService;

    @Transactional(rollbackFor = Exception.class)
    public PromptVersionVO createVersion(String taskCode, CreatePromptVersionRequest request) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        if (promptVersionRepo.findByTaskIdAndVersion(task.getId(), request.getVersion()).isPresent()) {
            throw new BusinessException("VERSION_EXISTS", "Prompt 版本已存在: " + request.getVersion());
        }
        LlmPromptVersionDO prompt = new LlmPromptVersionDO();
        prompt.setTaskId(task.getId());
        prompt.setVersion(request.getVersion());
        prompt.setTemplateBody(request.getTemplateBody());
        prompt.setOutputSchema(request.getOutputSchema());
        prompt.setStatus("DRAFT");
        prompt.setCreatedAt(LocalDateTime.now());
        prompt.setUpdatedAt(LocalDateTime.now());
        promptVersionRepo.insert(prompt);
        auditService.log("CREATE_VERSION", "PROMPT", taskCode,
                Map.of("version", request.getVersion()));
        return toVO(prompt);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptPublishResultVO rollback(String taskCode, String version) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        promptVersionRepo.findByTaskIdAndVersion(task.getId(), version)
                .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND",
                        "Prompt 版本不存在: " + taskCode + "@" + version));
        PromptPublishResultVO result = promptPublishService.publish(taskCode, version, null);
        auditService.log("ROLLBACK", "PROMPT", taskCode, Map.of("version", version));
        return result;
    }

    private LlmAiTaskDefDO requireTask(String taskCode) {
        return taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
    }

    private PromptVersionVO toVO(LlmPromptVersionDO prompt) {
        return PromptVersionVO.builder()
                .id(prompt.getId())
                .version(prompt.getVersion())
                .status(prompt.getStatus())
                .templateBody(prompt.getTemplateBody())
                .outputSchema(prompt.getOutputSchema())
                .publishedAt(prompt.getPublishedAt())
                .createdBy(prompt.getCreatedBy())
                .createdAt(prompt.getCreatedAt())
                .build();
    }
}
