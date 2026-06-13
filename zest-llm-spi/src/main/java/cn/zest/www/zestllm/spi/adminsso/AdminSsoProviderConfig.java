package cn.zest.www.zestllm.spi.adminsso;

import java.util.List;

/**
 * Admin SSO 运行时配置快照 — 由 {@code AdminSsoProperties} 实现。
 */
public interface AdminSsoProviderConfig {

    boolean isEnabled();

    String getProvider();

    String getDisplayName();

    String getIssuer();

    String getDiscoveryUri();

    String getClientId();

    String getClientSecret();

    String getRedirectUri();

    String getPostLogoutRedirectUri();

    List<String> getScopes();

    String getAudience();

    String getJwksUri();

    String getUsernameClaim();

    String getRolesClaim();

    String getAdminRole();

    String getOperatorRole();

    String getDefaultRole();

    boolean isZestSsoUseLogoutUrlApi();

    String getZestSsoLogoutUrlApiPath();
}
