package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.request.CreatePromptVersionRequest;
import cn.zest.www.zestllm.admin.model.request.ForkPromptVersionRequest;
import cn.zest.www.zestllm.admin.model.vo.PromptForkResultVO;
import cn.zest.www.zestllm.admin.model.vo.PromptPublishResultVO;
import cn.zest.www.zestllm.admin.model.vo.PromptVersionVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PromptVersionService {

    private static final Pattern VERSION_NUMBER = Pattern.compile("^v?(\\d+)$", Pattern.CASE_INSENSITIVE);

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

    public String suggestNextVersion(String taskCode) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        return suggestNextVersion(task.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptForkResultVO forkVersion(String taskCode, ForkPromptVersionRequest request) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        LlmPromptVersionDO base = promptVersionRepo.findByTaskIdAndVersion(task.getId(), request.getBaseVersion())
                .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND",
                        "Prompt 版本不存在: " + taskCode + "@" + request.getBaseVersion()));

        String newVersion = StringUtils.hasText(request.getVersion())
                ? request.getVersion().trim()
                : suggestNextVersion(task.getId());
        if (promptVersionRepo.findByTaskIdAndVersion(task.getId(), newVersion).isPresent()) {
            throw new BusinessException("VERSION_EXISTS", "Prompt 版本已存在: " + newVersion);
        }

        LlmPromptVersionDO prompt = new LlmPromptVersionDO();
        prompt.setTaskId(task.getId());
        prompt.setVersion(newVersion);
        prompt.setTemplateBody(request.getTemplateBody());
        prompt.setOutputSchema(StringUtils.hasText(request.getOutputSchema())
                ? request.getOutputSchema()
                : base.getOutputSchema());
        prompt.setStatus("DRAFT");
        prompt.setCreatedAt(LocalDateTime.now());
        prompt.setUpdatedAt(LocalDateTime.now());
        promptVersionRepo.insert(prompt);
        auditService.log("FORK_VERSION", "PROMPT", taskCode,
                Map.of("baseVersion", request.getBaseVersion(), "version", newVersion));

        LocalDateTime publishedAt = null;
        if (request.isPublish()) {
            PromptPublishResultVO published = promptPublishService.publish(taskCode, newVersion, null);
            publishedAt = published.getPublishedAt();
            prompt = promptVersionRepo.findByTaskIdAndVersion(task.getId(), newVersion).orElse(prompt);
        } else {
            prompt = promptVersionRepo.findByTaskIdAndVersion(task.getId(), newVersion).orElse(prompt);
        }

        return PromptForkResultVO.builder()
                .version(newVersion)
                .published(request.isPublish())
                .publishedAt(publishedAt)
                .versionDetail(toVO(prompt))
                .build();
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

    private String suggestNextVersion(Long taskId) {
        List<LlmPromptVersionDO> versions = promptVersionRepo.findByTaskId(taskId);
        int max = versions.stream()
                .map(LlmPromptVersionDO::getVersion)
                .map(this::parseVersionNumber)
                .max(Comparator.naturalOrder())
                .orElse(0);
        return "v" + (max + 1);
    }

    private int parseVersionNumber(String version) {
        if (!StringUtils.hasText(version)) {
            return 0;
        }
        Matcher matcher = VERSION_NUMBER.matcher(version.trim());
        if (!matcher.matches()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
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
