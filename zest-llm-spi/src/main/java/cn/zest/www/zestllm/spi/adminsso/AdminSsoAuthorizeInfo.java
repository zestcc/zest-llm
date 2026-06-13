package cn.zest.www.zestllm.spi.adminsso;

/**
 * PKCE 授权跳转信息。
 */
public record AdminSsoAuthorizeInfo(String authorizationUrl, String state) {
}
