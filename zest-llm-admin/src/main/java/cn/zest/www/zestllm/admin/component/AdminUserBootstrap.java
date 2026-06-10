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
        if (adminUserRepo.count() == 0) {
            insertAdmin();
            insertOperator();
            log.info("Bootstrapped default admin and operator users");
            return;
        }
        if (adminUserRepo.findByUsername("operator").isEmpty()) {
            insertOperator();
            log.info("Bootstrapped operator user");
        }
    }

    private void insertAdmin() {
        LlmAdminUserDO admin = new LlmAdminUserDO();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setDisplayName("Administrator");
        admin.setStatus("ACTIVE");
        admin.setRole("ADMIN");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.insert(admin);
    }

    private void insertOperator() {
        LlmAdminUserDO operator = new LlmAdminUserDO();
        operator.setUsername("operator");
        operator.setPasswordHash(passwordEncoder.encode("operator123"));
        operator.setDisplayName("Operator");
        operator.setStatus("ACTIVE");
        operator.setRole("OPERATOR");
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.insert(operator);
    }
}
