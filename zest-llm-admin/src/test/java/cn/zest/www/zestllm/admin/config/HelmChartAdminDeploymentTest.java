package cn.zest.www.zestllm.admin.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Helm Chart 静态冒烟：无需 helm CLI，校验 Admin Deployment 模板含 SSO/JWT env 片段。
 */
class HelmChartAdminDeploymentTest {

    @Test
    void adminDeployment_wiresSsoAndJwtEnvFromValues() throws Exception {
        Path chartRoot = findChartRoot();
        String deployment = Files.readString(chartRoot.resolve("templates/deployment-admin.yaml"));
        String adminEnv = Files.readString(chartRoot.resolve("templates/_admin-env.tpl"));
        String values = Files.readString(chartRoot.resolve("values.yaml"));

        assertTrue(deployment.contains("zest-llm.admin.ssoEnv"),
                "deployment-admin should include SSO env partial");
        assertTrue(deployment.contains("zest-llm.admin.jwtEnv"),
                "deployment-admin should include JWT env partial");
        assertTrue(deployment.contains("SPRING_DATA_REDIS_PORT"),
                "Redis port required for SSO PKCE / session revocation");

        assertTrue(adminEnv.contains("ZEST_LLM_ADMIN_SSO_ENABLED"));
        assertTrue(adminEnv.contains("ZEST_LLM_ADMIN_SSO_CLIENT_SECRET"));
        assertTrue(adminEnv.contains("secretKeyRef"));
        assertTrue(adminEnv.contains("ZEST_LLM_ADMIN_JWT_SECRET"));

        assertTrue(values.contains("admin:"));
        assertTrue(values.contains("sso:"));
        assertTrue(values.contains("existingSecret"));
    }

    private static Path findChartRoot() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            Path marker = dir.resolve("deploy/helm/zest-llm/Chart.yaml");
            if (Files.isRegularFile(marker)) {
                return dir.resolve("deploy/helm/zest-llm");
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("deploy/helm/zest-llm/Chart.yaml not found from cwd="
                + System.getProperty("user.dir"));
    }
}
