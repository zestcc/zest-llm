package cn.zest.www.zestllm.plugin.identity.zestsso;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 与 {@code zest-llm.admin.sso} 对齐的插件侧配置（用于同步到 ZestSSO Client SDK）。
 */
@Data
@ConfigurationProperties(prefix = "zest-llm.admin.sso")
public class ZestSsoPluginProperties {

    private boolean enabled;
    private String issuer = "http://localhost:9000";
    private String clientId;
    private String clientSecret;
    private String redirectUri = "http://localhost:5174/login/callback";
    private List<String> scopes = List.of("openid", "profile", "email", "roles", "tenant");
}
