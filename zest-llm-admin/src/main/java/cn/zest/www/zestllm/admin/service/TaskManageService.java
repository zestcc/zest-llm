package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.request.CreateTaskRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateTaskRequest;
import cn.zest.www.zestllm.admin.model.vo.TaskVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileProbeRepo;
import cn.zest.www.zestllm.admin.repo.LlmAgentProbeAlertRepo;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskManageService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAppRepo appRepo;
    private final LlmPromptVersionRepo promptVersionRepo;
    private final LlmModelRouteRepo modelRouteRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmAgentProfileProbeRepo agentProfileProbeRepo;
    private final LlmAgentProbeAlertRepo agentProbeAlertRepo;
    private final LlmExecutionRepo executionRepo;
    private final AuditService auditService;

    public Page<TaskVO> page(int pageNum, int pageSize, String appKey) {
        Long appId = null;
        if (StringUtils.hasText(appKey)) {
            appId = appRepo.findByAppKey(appKey)
                    .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在: " + appKey))
                    .getId();
        }
        Page<LlmAiTaskDefDO> page = taskDefRepo.page(pageNum, pageSize, appId);
        Page<TaskVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskVO create(CreateTaskRequest request) {
        LlmAppDO app = appRepo.findByAppKey(request.getAppKey())
                .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在: " + request.getAppKey()));
        if (taskDefRepo.findByAppIdAndCode(app.getId(), request.getCode()).isPresent()) {
            throw new BusinessException("TASK_EXISTS", "AI 作业已存在: " + request.getCode());
        }
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setAppId(app.getId());
        task.setCode(request.getCode());
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setStatus("ACTIVE");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskDefRepo.insert(task);
        auditService.log("CREATE", "TASK", request.getCode(),
                Map.of("appKey", request.getAppKey(), "name", request.getName()));
        return toVO(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskVO update(String code, UpdateTaskRequest request) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(code)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + code));
        if (StringUtils.hasText(request.getName())) {
            task.setName(request.getName());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getStatus())) {
            task.setStatus(request.getStatus());
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskDefRepo.update(task);
        auditService.log("UPDATE", "TASK", code, Map.of("name", task.getName(), "status", task.getStatus()));
        return toVO(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String code) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(code)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + code));
        List<String> blockers = collectDeleteBlockers(task);
        if (!blockers.isEmpty()) {
            throw new BusinessException("TASK_NOT_DELETABLE",
                    "无法删除作业 " + code + "：" + String.join("、", blockers));
        }
        taskDefRepo.deleteById(task.getId());
        auditService.log("DELETE", "TASK", code, Map.of("name", task.getName()));
    }

    /**
     * 按 taskId 强制删除（级联 Prompt / Route / Profile / Probe，不校验执行记录）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceDeleteById(Long taskId) {
        LlmAiTaskDefDO task = taskDefRepo.findById(taskId)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: id=" + taskId));
        cascadeDeleteTaskArtifacts(task.getId());
        taskDefRepo.deleteById(task.getId());
        auditService.log("FORCE_DELETE", "TASK", task.getCode(),
                Map.of("taskId", taskId, "name", task.getName()));
    }

    /**
     * 按 appKey + taskCode 强制删除（用于清理 order-service 等重复作业）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceDeleteByAppAndCode(String appKey, String code) {
        LlmAppDO app = appRepo.findByAppKey(appKey)
                .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在: " + appKey));
        LlmAiTaskDefDO task = taskDefRepo.findByAppIdAndCode(app.getId(), code)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND",
                        "AI 作业不存在: " + appKey + "/" + code));
        cascadeDeleteTaskArtifacts(task.getId());
        taskDefRepo.deleteById(task.getId());
        auditService.log("FORCE_DELETE", "TASK", code,
                Map.of("appKey", appKey, "taskId", task.getId(), "name", task.getName()));
    }

    private void cascadeDeleteTaskArtifacts(Long taskId) {
        agentProfileProbeRepo.deleteByTaskId(taskId);
        agentProbeAlertRepo.deleteByTaskId(taskId);
        agentProfileRepo.deleteByTaskId(taskId);
        promptVersionRepo.deleteByTaskId(taskId);
        modelRouteRepo.deleteByTaskId(taskId);
    }

    private List<String> collectDeleteBlockers(LlmAiTaskDefDO task) {
        List<String> blockers = new ArrayList<>();
        Long taskId = task.getId();
        if (!promptVersionRepo.findByTaskId(taskId).isEmpty()) {
            blockers.add("已配置 Prompt 版本");
        }
        if (modelRouteRepo.countByTaskId(taskId) > 0) {
            blockers.add("已配置模型路由");
        }
        if (!agentProfileRepo.findByTaskId(taskId).isEmpty()) {
            blockers.add("已配置 Agent Profile");
        }
        if (executionRepo.countByTaskCode(task.getCode()) > 0
                || executionRepo.countArchivedByTaskCode(task.getCode()) > 0) {
            blockers.add("存在执行记录");
        }
        return blockers;
    }

    private TaskVO toVO(LlmAiTaskDefDO task) {
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
}
