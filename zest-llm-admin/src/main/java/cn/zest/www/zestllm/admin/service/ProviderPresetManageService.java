package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmProviderPresetDO;
import cn.zest.www.zestllm.admin.model.entity.LlmTenantDO;
import cn.zest.www.zestllm.admin.model.request.CreateProviderPresetRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateProviderPresetRequest;
import cn.zest.www.zestllm.admin.model.vo.ProviderPresetVO;
import cn.zest.www.zestllm.admin.repo.LlmProviderPresetRepo;
import cn.zest.www.zestllm.admin.repo.LlmTenantRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProviderPresetManageService {

    private final LlmProviderPresetRepo providerPresetRepo;
    private final LlmTenantRepo tenantRepo;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public List<ProviderPresetVO> list() {
        return providerPresetRepo.findAllActive().stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderPresetVO create(CreateProviderPresetRequest request) {
        if (providerPresetRepo.findByCode(request.getPresetCode()).isPresent()) {
            throw new BusinessException("PRESET_EXISTS", "预设已存在: " + request.getPresetCode());
        }
        validateConfigJson(request.getConfigJson());
        LlmProviderPresetDO preset = new LlmProviderPresetDO();
        preset.setPresetCode(request.getPresetCode());
        preset.setPresetName(request.getPresetName());
        preset.setProviderType(StringUtils.hasText(request.getProviderType()) ? request.getProviderType() : "litellm");
        preset.setAuthMode(StringUtils.hasText(request.getAuthMode()) ? request.getAuthMode() : "API_KEY");
        preset.setConfigJson(request.getConfigJson());
        preset.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        preset.setStatus("ACTIVE");
        preset.setCreatedAt(LocalDateTime.now());
        preset.setUpdatedAt(LocalDateTime.now());
        if (StringUtils.hasText(request.getTenantCode())) {
            LlmTenantDO tenant = tenantRepo.findByTenantCode(request.getTenantCode())
                    .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "租户不存在"));
            preset.setTenantId(tenant.getId());
        }
        providerPresetRepo.insert(preset);
        auditService.log("CREATE", "PROVIDER_PRESET", request.getPresetCode(), Map.of());
        return toVO(preset);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProviderPresetVO update(String presetCode, UpdateProviderPresetRequest request) {
        LlmProviderPresetDO preset = providerPresetRepo.findByCode(presetCode)
                .orElseThrow(() -> new BusinessException("PRESET_NOT_FOUND", "预设不存在: " + presetCode));
        validateConfigJson(request.getConfigJson());
        preset.setPresetName(request.getPresetName());
        if (StringUtils.hasText(request.getProviderType())) {
            preset.setProviderType(request.getProviderType());
        }
        if (StringUtils.hasText(request.getAuthMode())) {
            preset.setAuthMode(request.getAuthMode());
        }
        preset.setConfigJson(request.getConfigJson());
        if (request.getSortOrder() != null) {
            preset.setSortOrder(request.getSortOrder());
        }
        if (StringUtils.hasText(request.getStatus())) {
            preset.setStatus(request.getStatus());
        }
        preset.setUpdatedAt(LocalDateTime.now());
        providerPresetRepo.update(preset);
        auditService.log("UPDATE", "PROVIDER_PRESET", presetCode, Map.of());
        return toVO(preset);
    }

    private void validateConfigJson(String configJson) {
        try {
            objectMapper.readTree(configJson);
        } catch (Exception ex) {
            throw new BusinessException("INVALID_CONFIG", "configJson 不是合法 JSON");
        }
    }

    private ProviderPresetVO toVO(LlmProviderPresetDO preset) {
        return ProviderPresetVO.builder()
                .id(preset.getId())
                .presetCode(preset.getPresetCode())
                .presetName(preset.getPresetName())
                .providerType(preset.getProviderType())
                .authMode(preset.getAuthMode())
                .configJson(preset.getConfigJson())
                .sortOrder(preset.getSortOrder())
                .status(preset.getStatus())
                .updatedAt(preset.getUpdatedAt())
                .build();
    }
}
