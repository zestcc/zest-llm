package cn.zest.www.zestllm.admin.service.sso.oidc;

/**
 * OIDC 端点集合（Discovery 或静态配置解析结果）。
 */
public record OidcEndpoints(
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwksUri,
        String endSessionEndpoint
) {
}
