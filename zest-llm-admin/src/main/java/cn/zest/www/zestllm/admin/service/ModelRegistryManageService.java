package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import cn.zest.www.zestllm.admin.model.request.CreateGatewayModelRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateGatewayModelRequest;
import cn.zest.www.zestllm.admin.model.vo.GatewayModelVO;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.admin.repo.LlmSecretRefRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ModelRegistryManageService {

    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$");

    private final LlmGatewayModelRepo gatewayModelRepo;
    private final LlmSecretRefRepo secretRefRepo;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public List<GatewayModelVO> list() {
        return gatewayModelRepo.findAllActive().stream().map(this::toVO).toList();
    }

    public GatewayModelVO getByModelName(String modelName) {
        return toVO(requireModel(modelName));
    }

    @Transactional(rollbackFor = Exception.class)
    public GatewayModelVO create(CreateGatewayModelRequest request) {
        validateModelName(request.getModelName());
        if (gatewayModelRepo.findByModelName(request.getModelName()).isPresent()) {
            throw new BusinessException("MODEL_EXISTS", "模型已存在: " + request.getModelName());
        }
        validateSecretRef(request.getApiKeySecretRef());
        validateExtraJson(request.getExtraJson());

        LlmGatewayModelDO entity = new LlmGatewayModelDO();
        entity.setModelName(request.getModelName());
        entity.setUpstreamModel(request.getUpstreamModel());
        entity.setApiBase(request.getApiBase());
        entity.setApiKeySecretRef(request.getApiKeySecretRef());
        entity.setExtraJson(request.getExtraJson());
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus("ACTIVE");
        entity.setSyncStatus("PENDING");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        gatewayModelRepo.insert(entity);
        auditService.log("CREATE", "GATEWAY_MODEL", request.getModelName(), Map.of());
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public GatewayModelVO update(String modelName, UpdateGatewayModelRequest request) {
        LlmGatewayModelDO entity = requireModel(modelName);
        if (StringUtils.hasText(request.getUpstreamModel())) {
            entity.setUpstreamModel(request.getUpstreamModel());
        }
        if (request.getApiBase() != null) {
            entity.setApiBase(request.getApiBase());
        }
        if (request.getApiKeySecretRef() != null) {
            validateSecretRef(StringUtils.hasText(request.getApiKeySecretRef()) ? request.getApiKeySecretRef() : null);
            entity.setApiKeySecretRef(request.getApiKeySecretRef());
        }
        if (request.getExtraJson() != null) {
            validateExtraJson(request.getExtraJson());
            entity.setExtraJson(request.getExtraJson());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
        entity.setSyncStatus("PENDING");
        entity.setUpdatedAt(LocalDateTime.now());
        gatewayModelRepo.update(entity);
        auditService.log("UPDATE", "GATEWAY_MODEL", modelName, Map.of());
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public GatewayModelVO upsertForImport(CreateGatewayModelRequest request) {
        validateModelName(request.getModelName());
        validateSecretRef(request.getApiKeySecretRef());
        validateExtraJson(request.getExtraJson());

        return gatewayModelRepo.findByModelName(request.getModelName())
                .map(existing -> {
                    UpdateGatewayModelRequest update = new UpdateGatewayModelRequest();
                    update.setUpstreamModel(request.getUpstreamModel());
                    update.setApiBase(request.getApiBase());
                    update.setApiKeySecretRef(request.getApiKeySecretRef());
                    update.setExtraJson(request.getExtraJson());
                    update.setSortOrder(request.getSortOrder());
                    return update(request.getModelName(), update);
                })
                .orElseGet(() -> create(request));
    }

    public void validateModelName(String modelName) {
        if (!StringUtils.hasText(modelName)) {
            throw new BusinessException("INVALID_MODEL_NAME", "model_name 不能为空");
        }
        if (!MODEL_NAME_PATTERN.matcher(modelName).matches()) {
            throw new BusinessException("INVALID_MODEL_NAME",
                    "model_name 仅允许字母数字及 ._- 且以字母数字开头: " + modelName);
        }
    }

    LlmGatewayModelDO requireModel(String modelName) {
        return gatewayModelRepo.findByModelName(modelName)
                .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "模型不存在: " + modelName));
    }

    private void validateSecretRef(String secretRef) {
        if (StringUtils.hasText(secretRef) && secretRefRepo.findByCode(secretRef).isEmpty()) {
            throw new BusinessException("SECRET_REF_NOT_FOUND", "密钥引用不存在: " + secretRef);
        }
    }

    private void validateExtraJson(String extraJson) {
        if (!StringUtils.hasText(extraJson)) {
            return;
        }
        try {
            objectMapper.readTree(extraJson);
        } catch (Exception ex) {
            throw new BusinessException("INVALID_EXTRA_JSON", "extraJson 不是合法 JSON");
        }
    }

    GatewayModelVO toVO(LlmGatewayModelDO entity) {
        return GatewayModelVO.builder()
                .id(entity.getId())
                .modelName(entity.getModelName())
                .upstreamModel(entity.getUpstreamModel())
                .apiBase(entity.getApiBase())
                .apiKeySecretRef(entity.getApiKeySecretRef())
                .extraJson(entity.getExtraJson())
                .status(entity.getStatus())
                .syncStatus(entity.getSyncStatus())
                .lastSyncAt(entity.getLastSyncAt())
                .sortOrder(entity.getSortOrder())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
