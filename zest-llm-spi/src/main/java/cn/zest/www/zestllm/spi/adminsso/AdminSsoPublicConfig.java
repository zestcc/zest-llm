package cn.zest.www.zestllm.spi.adminsso;

/**
 * SSO 公开配置（前端可见，不含密钥）。
 */
public record AdminSsoPublicConfig(
        boolean enabled,
        String provider,
        String displayName,
        String issuer,
        String clientId
) {
}
