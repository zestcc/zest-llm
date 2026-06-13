package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest-llm.admin.oidc")
public class AdminOidcProperties {
    private boolean enabled = false;
    private String issuer;
    private String audience;
    private String jwksUri;
    private String clientId;
    private String clientSecret;
    private String redirectUri = "http://localhost:5174/login/callback";
    private String postLogoutRedirectUri = "http://localhost:5174/login";
    private java.util.List<String> scopes = java.util.List.of("openid", "profile", "email", "roles", "tenant");
    private String usernameClaim = "preferred_username";
    private String rolesClaim = "roles";
    private String defaultRole = "ADMIN";
}
