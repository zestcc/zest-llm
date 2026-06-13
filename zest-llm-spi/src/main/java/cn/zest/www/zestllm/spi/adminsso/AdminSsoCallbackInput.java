package cn.zest.www.zestllm.spi.adminsso;

/**
 * OIDC 授权码回调参数。
 */
public record AdminSsoCallbackInput(String code, String state) {
}
