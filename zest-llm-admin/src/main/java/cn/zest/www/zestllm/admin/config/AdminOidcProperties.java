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
    /** 允许登录的 Admin 用户名 claim，默认 preferred_username */
    private String usernameClaim = "preferred_username";
    /** 允许登录的角色 claim 值映射为 ADMIN */
    private String defaultRole = "ADMIN";
}
