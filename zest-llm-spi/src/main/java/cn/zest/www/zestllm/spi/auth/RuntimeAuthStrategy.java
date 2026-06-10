package cn.zest.www.zestllm.spi.auth;

/**
 * Runtime 入站鉴权策略 SPI（STATIC_TOKEN / OIDC_JWT / API_KEY）。
 */
public interface RuntimeAuthStrategy {

    String mode();

    void authenticate(RuntimeAuthContext context);
}
