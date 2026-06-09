package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmTenantDO;
import cn.zest.www.zestllm.admin.model.request.CreateTenantRequest;
import cn.zest.www.zestllm.admin.model.vo.TenantVO;
import cn.zest.www.zestllm.admin.repo.LlmTenantRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantManageService {

    private final LlmTenantRepo tenantRepo;
    private final AuditService auditService;

    public List<TenantVO> listAll() {
        return tenantRepo.findAll().stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantVO create(CreateTenantRequest request) {
        tenantRepo.findByTenantCode(request.getTenantCode()).ifPresent(t -> {
            throw new BusinessException("TENANT_EXISTS", "租户已存在: " + request.getTenantCode());
        });
        LlmTenantDO tenant = new LlmTenantDO();
        tenant.setTenantCode(request.getTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setStatus("ACTIVE");
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepo.insert(tenant);
        auditService.log("CREATE", "TENANT", tenant.getTenantCode(),
                java.util.Map.of("tenantName", tenant.getTenantName()));
        return toVO(tenant);
    }

    private TenantVO toVO(LlmTenantDO tenant) {
        return TenantVO.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .tenantName(tenant.getTenantName())
                .status(tenant.getStatus())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}
