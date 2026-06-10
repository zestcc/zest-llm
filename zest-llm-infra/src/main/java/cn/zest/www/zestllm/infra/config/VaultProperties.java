package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.vault")
public class VaultProperties {
    private String address = "http://127.0.0.1:8200";
    private String token;
    private String mount = "secret";
    private String apiVersion = "v2";
}
