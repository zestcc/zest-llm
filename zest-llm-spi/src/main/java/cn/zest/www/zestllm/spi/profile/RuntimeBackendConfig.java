package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

/**
 * Profile extensions.runtimeBackend — 见 docs/schemas/profile-extensions-v1.1.json
 */
@Data
public class RuntimeBackendConfig {

    private String type = "native";
    private String baseUrl;
    private String externalAppId;
    private String protocol = "openai-compatible";
    private String secretRef;
    private Integer timeoutMs = 60000;
}
