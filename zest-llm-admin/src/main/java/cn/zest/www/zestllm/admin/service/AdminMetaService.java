package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.AdminBuildInfoVO;
import cn.zest.www.zestllm.admin.model.vo.AdminFeaturesVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMetaService {

    private static final String FLYWAY_LATEST = "V19";

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final Optional<BuildProperties> buildProperties;
    private final Optional<GitProperties> gitProperties;

    @Value("${spring.application.name:zest-llm-admin}")
    private String applicationName;

    @Value("${zest-llm.admin.meta.app-version:1.0.0}")
    private String appVersion;

    public AdminFeaturesVO features() {
        Map<String, Boolean> schemaReady = new LinkedHashMap<>();
        schemaReady.put("llm_agent_profile_probe", tableExists("llm_agent_profile_probe"));
        schemaReady.put("llm_agent_probe_alert", tableExists("llm_agent_probe_alert"));
        schemaReady.put("llm_execution_archive_run", tableExists("llm_execution_archive_run"));
        schemaReady.put("llm_learning_cycle_run", tableExists("llm_learning_cycle_run"));
        boolean agentProbeApi = Boolean.TRUE.equals(schemaReady.get("llm_agent_profile_probe"));
        boolean learningApi = Boolean.TRUE.equals(schemaReady.get("llm_learning_cycle_run"));
        return AdminFeaturesVO.builder()
                .appVersion(appVersion)
                .flywayLatestScript(FLYWAY_LATEST)
                .agentProbeApi(agentProbeApi)
                .learningApi(learningApi)
                .capabilityStackApi(true)
                .scenarioTemplateApi(true)
                .integrationAdaptersEnabled(true)
                .schemaReady(schemaReady)
                .build();
    }

    public AdminBuildInfoVO buildInfo() {
        String profiles = Arrays.stream(environment.getActiveProfiles())
                .filter(p -> !p.isBlank())
                .collect(Collectors.joining(","));
        if (profiles.isBlank()) {
            profiles = "default";
        }
        return AdminBuildInfoVO.builder()
                .appVersion(appVersion)
                .artifactId(applicationName)
                .flywayLatestScript(FLYWAY_LATEST)
                .activeProfiles(profiles)
                .javaVersion(System.getProperty("java.version"))
                .gitCommit(resolveGitCommit())
                .buildTime(resolveBuildTime())
                .build();
    }

    private String resolveGitCommit() {
        return gitProperties.map(GitProperties::getShortCommitId)
                .or(() -> buildProperties.map(BuildProperties::getVersion))
                .orElse("unknown");
    }

    private String resolveBuildTime() {
        if (buildProperties.isPresent()) {
            Instant time = buildProperties.get().getTime();
            if (time != null) {
                return time.toString();
            }
        }
        return gitProperties.map(GitProperties::getCommitTime)
                .map(Instant::toString)
                .orElse("unknown");
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
