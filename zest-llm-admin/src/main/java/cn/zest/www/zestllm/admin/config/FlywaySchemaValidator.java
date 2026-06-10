package cn.zest.www.zestllm.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlywaySchemaValidator implements ApplicationRunner {

    private static final String[] REQUIRED_TABLES = {
            "llm_agent_profile_probe",
            "llm_agent_probe_alert",
            "llm_learning_cycle_run"
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String table : REQUIRED_TABLES) {
            if (!tableExists(table)) {
                log.error("Flyway schema incomplete: missing table `{}`. "
                        + "Run Admin with spring.profiles.active=local/docker and ensure migrations V15/V16/V17 applied.",
                        table);
            }
        }
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                    Integer.class,
                    tableName);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
