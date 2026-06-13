package cn.zest.www.zestllm.spi.adminsso;

/**
 * SSO 登录成功结果（由 Admin 层转换为 JWT 响应）。
 */
public record AdminSsoLoginResult(String username, String role, String subject) {
}
