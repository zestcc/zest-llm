package cn.zest.www.zestllm.admin.component;

import cn.zest.www.zestllm.admin.model.entity.LlmAdminUserDO;
import cn.zest.www.zestllm.admin.repo.LlmAdminUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserBootstrap implements ApplicationRunner {

    private final LlmAdminUserRepo adminUserRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepo.count() > 0) {
            return;
        }
        LlmAdminUserDO admin = new LlmAdminUserDO();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setDisplayName("Administrator");
        admin.setStatus("ACTIVE");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.insert(admin);
        log.info("Bootstrapped default admin user");
    }
}
