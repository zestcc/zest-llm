package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.JwtProperties;
import cn.zest.www.zestllm.admin.config.JwtTokenProvider;
import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAdminUserDO;
import cn.zest.www.zestllm.admin.model.request.AdminLoginRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
import cn.zest.www.zestllm.admin.repo.LlmAdminUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final LlmAdminUserRepo adminUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AdminLoginVO login(AdminLoginRequest request) {
        LlmAdminUserDO user = adminUserRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> BusinessException.unauthorized("用户名或密码错误"));
        if (!"ACTIVE".equals(user.getStatus())) {
            throw BusinessException.unauthorized("账号已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        String token = jwtTokenProvider.createToken(user.getUsername());
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtProperties.getExpirationMs() / 1000);
        vo.setUsername(user.getUsername());
        return vo;
    }
}
