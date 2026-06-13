package cn.zest.www.zestllm.admin.service.sso;

import cn.zest.www.zestllm.admin.config.JwtProperties;
import cn.zest.www.zestllm.admin.config.JwtTokenProvider;
import cn.zest.www.zestllm.admin.model.entity.LlmAdminUserDO;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
import cn.zest.www.zestllm.admin.repo.LlmAdminUserRepo;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoLoginResult;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProviderConfig;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SSO 用户入库与 JWT 签发。
 */
@Component
@RequiredArgsConstructor
public class AdminSsoUserProvisioner {

    private final LlmAdminUserRepo adminUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AdminSessionRevocationService sessionRevocationService;

    public AdminSsoLoginResult provisionFromClaims(String providerId, Claims claims, AdminSsoProviderConfig config) {
        String subject = claims.getSubject();
        String username = readClaim(claims, config.getUsernameClaim());
        if (!StringUtils.hasText(username)) {
            username = subject;
        }
        String role = mapRole(claims, config);
        String displayName = readClaim(claims, "name");
        LlmAdminUserDO user = provisionUser(providerId, subject, username, role, displayName);
        return new AdminSsoLoginResult(user.getUsername(), user.getRole(), subject);
    }

    public AdminLoginVO toLoginVo(AdminSsoLoginResult result) {
        if (sessionRevocationService != null) {
            sessionRevocationService.clearRevocation(result.username());
        }
        String token = jwtTokenProvider.createToken(result.username(), result.role());
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtProperties.getExpirationMs() / 1000);
        vo.setUsername(result.username());
        vo.setRole(result.role());
        return vo;
    }

    private LlmAdminUserDO provisionUser(String providerId, String subject, String username, String role, String displayName) {
        return adminUserRepo.findBySsoSubject(providerId, subject)
                .or(() -> adminUserRepo.findByUsername(username))
                .map(existing -> {
                    existing.setSsoProvider(providerId);
                    existing.setSsoSubject(subject);
                    existing.setRole(role);
                    if (StringUtils.hasText(displayName)) {
                        existing.setDisplayName(displayName);
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    adminUserRepo.update(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    LlmAdminUserDO user = new LlmAdminUserDO();
                    user.setUsername(username);
                    user.setSsoProvider(providerId);
                    user.setSsoSubject(subject);
                    user.setDisplayName(StringUtils.hasText(displayName) ? displayName : username);
                    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setStatus("ACTIVE");
                    user.setRole(role);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    adminUserRepo.insert(user);
                    return user;
                });
    }

    private String mapRole(Claims claims, AdminSsoProviderConfig config) {
        Object roles = claims.get(config.getRolesClaim());
        if (roles instanceof List<?> list) {
            for (Object r : list) {
                String code = String.valueOf(r);
                if (config.getAdminRole().equals(code) || "ADMIN".equals(code)) {
                    return "ADMIN";
                }
                if (config.getOperatorRole().equals(code) || "OPERATOR".equals(code)) {
                    return "OPERATOR";
                }
            }
        }
        return config.getDefaultRole();
    }

    private String readClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value != null ? String.valueOf(value) : null;
    }
}
