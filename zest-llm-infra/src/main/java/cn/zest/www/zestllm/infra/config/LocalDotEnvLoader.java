package cn.zest.www.zestllm.infra.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地开发：启动前加载 deploy/litellm/.env 到 System properties，
 * 供 EnvSecretResolver（env:XXX）与 LiteLLM 共用。
 */
public final class LocalDotEnvLoader {

    private LocalDotEnvLoader() {
    }

    public static int loadIfPresent() {
        Path envFile = resolveEnvFile();
        if (envFile == null || !Files.isRegularFile(envFile)) {
            return 0;
        }
        int loaded = 0;
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isBlank() && !value.isBlank()) {
                    System.setProperty(key, value);
                    loaded++;
                }
            }
        } catch (Exception ignored) {
            return 0;
        }
        return loaded;
    }

    private static Path resolveEnvFile() {
        Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(Paths.get("deploy", "litellm", ".env"));
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }
}
