package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAdminUserDO;
import cn.zest.www.zestllm.admin.model.request.CreateAdminUserRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAdminUserRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminUserVO;
import cn.zest.www.zestllm.admin.repo.LlmAdminUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserManageService {

    private final LlmAdminUserRepo adminUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public List<AdminUserVO> list() {
        return adminUserRepo.findAll().stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO create(CreateAdminUserRequest request) {
        if (adminUserRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("USER_EXISTS", "用户名已存在: " + request.getUsername());
        }
        LlmAdminUserDO user = new LlmAdminUserDO();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(StringUtils.hasText(request.getDisplayName())
                ? request.getDisplayName() : request.getUsername());
        user.setRole(StringUtils.hasText(request.getRole()) ? request.getRole() : "OPERATOR");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.insert(user);
        auditService.log("CREATE", "ADMIN_USER", request.getUsername(), Map.of("role", user.getRole()));
        return toVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO update(String username, UpdateAdminUserRequest request) {
        LlmAdminUserDO user = adminUserRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在: " + username));
        if (StringUtils.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName());
        }
        if (StringUtils.hasText(request.getRole())) {
            user.setRole(request.getRole());
        }
        if (StringUtils.hasText(request.getStatus())) {
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.update(user);
        auditService.log("UPDATE", "ADMIN_USER", username, Map.of());
        return toVO(user);
    }

    private AdminUserVO toVO(LlmAdminUserDO user) {
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
