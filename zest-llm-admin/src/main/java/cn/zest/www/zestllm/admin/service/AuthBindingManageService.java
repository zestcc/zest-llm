package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAuthBindingDO;
import cn.zest.www.zestllm.admin.model.request.UpsertAuthBindingRequest;
import cn.zest.www.zestllm.admin.model.vo.AuthBindingVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmAuthBindingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthBindingManageService {

    private final LlmAuthBindingRepo authBindingRepo;
    private final LlmAppRepo appRepo;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final AuditService auditService;

    public AuthBindingVO getByAppKey(String appKey) {
        LlmAppDO app = appRepo.findByAppKey(appKey)
                .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在"));
        return authBindingRepo.findByScope("APP", app.getId())
                .map(binding -> toVO(binding, appKey, null))
                .orElse(AuthBindingVO.builder()
                        .scopeType("APP")
                        .scopeId(app.getId())
                        .appKey(appKey)
                        .inboundMode(app.getAuthMode() != null ? app.getAuthMode() : "STATIC_TOKEN")
                        .inboundConfigJson(app.getAuthConfigJson())
                        .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthBindingVO upsert(UpsertAuthBindingRequest request) {
        Long scopeId = resolveScopeId(request);
        String scopeType = request.getScopeType().toUpperCase();
        LlmAuthBindingDO binding = authBindingRepo.findByScope(scopeType, scopeId).orElseGet(() -> {
            LlmAuthBindingDO created = new LlmAuthBindingDO();
            created.setScopeType(scopeType);
            created.setScopeId(scopeId);
            created.setStatus("ACTIVE");
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        binding.setInboundMode(request.getInboundMode());
        binding.setInboundConfigJson(request.getInboundConfigJson());
        binding.setOutboundMode(request.getOutboundMode());
        binding.setOutboundConfigJson(request.getOutboundConfigJson());
        binding.setUpdatedAt(LocalDateTime.now());
        if (binding.getId() == null) {
            authBindingRepo.insert(binding);
        } else {
            authBindingRepo.update(binding);
        }
        if ("APP".equals(scopeType)) {
            LlmAppDO app = appRepo.findById(scopeId)
                    .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在"));
            app.setAuthMode(request.getInboundMode());
            app.setAuthConfigJson(request.getInboundConfigJson());
            app.setUpdatedAt(LocalDateTime.now());
            appRepo.update(app);
        }
        auditService.log("UPSERT", "AUTH_BINDING", scopeType + ":" + scopeId, Map.of("inboundMode", request.getInboundMode()));
        String appKey = request.getAppKey();
        String taskCode = request.getTaskCode();
        return toVO(binding, appKey, taskCode);
    }

    private Long resolveScopeId(UpsertAuthBindingRequest request) {
        if (request.getScopeId() != null) {
            return request.getScopeId();
        }
        if ("APP".equalsIgnoreCase(request.getScopeType())) {
            if (!StringUtils.hasText(request.getAppKey())) {
                throw new BusinessException("APP_KEY_REQUIRED", "appKey 必填");
            }
            return appRepo.findByAppKey(request.getAppKey())
                    .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在"))
                    .getId();
        }
        if ("TASK".equalsIgnoreCase(request.getScopeType())) {
            if (!StringUtils.hasText(request.getTaskCode())) {
                throw new BusinessException("TASK_CODE_REQUIRED", "taskCode 必填");
            }
            return taskDefRepo.findByCode(request.getTaskCode())
                    .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "作业不存在"))
                    .getId();
        }
        throw new BusinessException("INVALID_SCOPE", "不支持的 scopeType");
    }

    private AuthBindingVO toVO(LlmAuthBindingDO binding, String appKey, String taskCode) {
        return AuthBindingVO.builder()
                .id(binding.getId())
                .scopeType(binding.getScopeType())
                .scopeId(binding.getScopeId())
                .appKey(appKey)
                .taskCode(taskCode)
                .inboundMode(binding.getInboundMode())
                .inboundConfigJson(binding.getInboundConfigJson())
                .outboundMode(binding.getOutboundMode())
                .outboundConfigJson(binding.getOutboundConfigJson())
                .status(binding.getStatus())
                .updatedAt(binding.getUpdatedAt())
                .build();
    }
}
