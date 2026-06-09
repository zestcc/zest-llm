package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.request.CreateTaskRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateTaskRequest;
import cn.zest.www.zestllm.admin.model.vo.TaskVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskManageService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAppRepo appRepo;
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
