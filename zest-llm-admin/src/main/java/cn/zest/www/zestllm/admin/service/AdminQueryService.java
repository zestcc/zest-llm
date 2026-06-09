package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.vo.AppVO;
import cn.zest.www.zestllm.admin.model.vo.PromptVersionVO;
import cn.zest.www.zestllm.admin.model.vo.TaskVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQueryService {

    private final LlmAppRepo appRepo;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmPromptVersionRepo promptVersionRepo;

    public List<AppVO> listApps() {
        return appRepo.findAll().stream()
                .map(this::toAppVO)
                .toList();
    }

    public List<TaskVO> listTasks() {
        return taskDefRepo.findAll().stream()
                .map(this::toTaskVO)
                .toList();
    }

    public List<PromptVersionVO> listPromptVersions(String taskCode) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        return promptVersionRepo.findByTaskId(task.getId()).stream()
                .map(this::toPromptVO)
                .toList();
    }

    private AppVO toAppVO(LlmAppDO app) {
        return AppVO.builder()
                .id(app.getId())
                .appKey(app.getAppKey())
                .appName(app.getAppName())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .build();
    }

    private TaskVO toTaskVO(LlmAiTaskDefDO task) {
        String appKey = appRepo.findById(task.getAppId()).map(LlmAppDO::getAppKey).orElse(null);
        return TaskVO.builder()
                .id(task.getId())
                .appKey(appKey)
                .code(task.getCode())
                .name(task.getName())
                .description(task.getDescription())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private PromptVersionVO toPromptVO(LlmPromptVersionDO prompt) {
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
