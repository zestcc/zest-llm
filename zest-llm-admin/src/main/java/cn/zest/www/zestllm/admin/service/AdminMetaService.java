package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.AdminFeaturesVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMetaService {

    private static final String FLYWAY_LATEST = "V18";

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:zest-llm-admin}")
    private String applicationName;

    public AdminFeaturesVO features() {
        Map<String, Boolean> schemaReady = new LinkedHashMap<>();
        schemaReady.put("llm_agent_profile_probe", tableExists("llm_agent_profile_probe"));
        schemaReady.put("llm_agent_probe_alert", tableExists("llm_agent_probe_alert"));
        schemaReady.put("llm_execution_archive_run", tableExists("llm_execution_archive_run"));
        schemaReady.put("llm_learning_cycle_run", tableExists("llm_learning_cycle_run"));
        boolean agentProbeApi = Boolean.TRUE.equals(schemaReady.get("llm_agent_profile_probe"));
        boolean learningApi = Boolean.TRUE.equals(schemaReady.get("llm_learning_cycle_run"));
        return AdminFeaturesVO.builder()
                .appVersion(applicationName)
                .flywayLatestScript(FLYWAY_LATEST)
                .agentProbeApi(agentProbeApi)
                .learningApi(learningApi)
                .capabilityStackApi(true)
                .scenarioTemplateApi(true)
                .integrationAdaptersEnabled(true)
                .schemaReady(schemaReady)
                .build();
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                    Integer.class,
                    tableName);
            return count != null && count > 0;
        } catch (Exception ex) {
            log.debug("Failed to check table {}", tableName, ex);
            return false;
        }
    }
}
