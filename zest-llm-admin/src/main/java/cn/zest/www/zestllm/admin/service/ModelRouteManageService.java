package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.request.UpdateModelRouteRequest;
import cn.zest.www.zestllm.admin.model.vo.ModelRouteVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModelRouteManageService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmModelRouteRepo modelRouteRepo;
    private final LlmAppRepo appRepo;
    private final PolicyCacheAdapter policyCacheAdapter;
    private final AuditService auditService;

    public List<ModelRouteVO> list(String taskCode) {
        if (StringUtils.hasText(taskCode)) {
            LlmAiTaskDefDO task = requireTask(taskCode);
            return modelRouteRepo.findByTaskId(task.getId())
                    .map(route -> List.of(toVO(task.getCode(), route)))
                    .orElse(List.of());
        }
        List<LlmAiTaskDefDO> tasks = taskDefRepo.findAll();
        List<Long> taskIds = tasks.stream().map(LlmAiTaskDefDO::getId).toList();
        Map<Long, String> codeById = tasks.stream()
                .collect(java.util.stream.Collectors.toMap(LlmAiTaskDefDO::getId, LlmAiTaskDefDO::getCode));
        return modelRouteRepo.findByTaskIds(taskIds).stream()
                .map(route -> toVO(codeById.get(route.getTaskId()), route))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelRouteVO update(String taskCode, UpdateModelRouteRequest request) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        LlmModelRouteDO route = modelRouteRepo.findByTaskId(task.getId()).orElseGet(() -> {
            LlmModelRouteDO created = new LlmModelRouteDO();
            created.setTaskId(task.getId());
            created.setStatus("ACTIVE");
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        route.setPrimaryModel(request.getPrimaryModel());
        route.setFallbackModels(request.getFallbackModels());
        route.setMaxTokens(request.getMaxTokens());
        route.setTemperature(request.getTemperature());
        route.setTimeoutMs(request.getTimeoutMs());
        route.setPolicyJson(request.getPolicyJson());
        route.setUpdatedAt(LocalDateTime.now());
        if (route.getId() == null) {
            modelRouteRepo.insert(route);
        } else {
            modelRouteRepo.update(route);
        }
        auditService.log("UPDATE", "MODEL_ROUTE", taskCode,
                Map.of("primaryModel", request.getPrimaryModel()));
        appRepo.findById(task.getAppId()).ifPresent(app ->
                policyCacheAdapter.invalidate(app.getAppKey(), task.getCode()));
        return toVO(taskCode, route);
    }

    private LlmAiTaskDefDO requireTask(String taskCode) {
        return taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
    }

    private ModelRouteVO toVO(String taskCode, LlmModelRouteDO route) {
        return ModelRouteVO.builder()
                .id(route.getId())
                .taskCode(taskCode)
                .primaryModel(route.getPrimaryModel())
                .fallbackModels(route.getFallbackModels())
                .maxTokens(route.getMaxTokens())
                .temperature(route.getTemperature())
                .timeoutMs(route.getTimeoutMs())
                .policyJson(route.getPolicyJson())
                .status(route.getStatus())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
