package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmTenantDO;
import cn.zest.www.zestllm.admin.model.request.CreateAppRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAppRequest;
import cn.zest.www.zestllm.admin.model.vo.AppVO;
import cn.zest.www.zestllm.admin.model.vo.RotateTokenVO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmTenantRepo;
import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppManageService {

    private static final String DEFAULT_TENANT_CODE = "zest-demo";

    private final LlmAppRepo appRepo;
    private final LlmTenantRepo tenantRepo;
    private final AuditService auditService;

    public Page<AppVO> page(int pageNum, int pageSize) {
        Page<LlmAppDO> page = appRepo.page(pageNum, pageSize);
        Page<AppVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public AppVO create(CreateAppRequest request) {
        if (appRepo.findByAppKey(request.getAppKey()).isPresent()) {
            throw new BusinessException("APP_EXISTS", "应用已存在: " + request.getAppKey());
        }
        LlmTenantDO tenant = resolveTenant(request.getTenantCode());
        String rawToken = TokenHashUtil.generateRawToken();
        LlmAppDO app = new LlmAppDO();
        app.setTenantId(tenant.getId());
        app.setAppKey(request.getAppKey());
        app.setAppName(request.getAppName());
        app.setTokenHash(TokenHashUtil.encodeToken(rawToken));
        app.setAuthMode("STATIC_TOKEN");
        app.setAuthConfigJson("{\"mode\":\"STATIC_TOKEN\"}");
        app.setStatus("ACTIVE");
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.insert(app);
        auditService.log("CREATE", "APP", app.getAppKey(), Map.of("appName", app.getAppName()));
        return toVO(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public AppVO update(String appKey, UpdateAppRequest request) {
        LlmAppDO app = requireApp(appKey);
        if (StringUtils.hasText(request.getAppName())) {
            app.setAppName(request.getAppName());
        }
        if (StringUtils.hasText(request.getStatus())) {
            app.setStatus(request.getStatus());
        }
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.update(app);
        auditService.log("UPDATE", "APP", appKey, Map.of("appName", app.getAppName(), "status", app.getStatus()));
        return toVO(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public RotateTokenVO rotateToken(String appKey) {
        LlmAppDO app = requireApp(appKey);
        String rawToken = TokenHashUtil.generateRawToken();
        app.setTokenHash(TokenHashUtil.encodeToken(rawToken));
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.update(app);
        auditService.log("ROTATE_TOKEN", "APP", appKey, Map.of());
        return RotateTokenVO.builder().appKey(appKey).rawToken(rawToken).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(String appKey) {
        LlmAppDO app = requireApp(appKey);
        app.setStatus("DISABLED");
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.update(app);
        auditService.log("DISABLE", "APP", appKey, Map.of());
    }

    private LlmTenantDO resolveTenant(String tenantCode) {
        String code = StringUtils.hasText(tenantCode) ? tenantCode : DEFAULT_TENANT_CODE;
        return tenantRepo.findByTenantCode(code)
                .or(() -> tenantRepo.findById(1L))
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "租户不存在: " + code));
    }

    private LlmAppDO requireApp(String appKey) {
        return appRepo.findByAppKey(appKey)
                .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在: " + appKey));
    }

    private AppVO toVO(LlmAppDO app) {
        return AppVO.builder()
                .id(app.getId())
                .appKey(app.getAppKey())
                .appName(app.getAppName())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .build();
    }
}
