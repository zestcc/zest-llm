package cn.zest.www.zestllm.common.util;

/**
 * 生产环境密钥/口令强度校验（零 Spring 依赖，Admin 启动守卫共用）。
 */
public final class ProductionSecretGuard {

    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_JWT_MARKER = "Change_Me_In_Production";
    private static final int MIN_MACHINE_TOKEN_LENGTH = 16;
    private static final int MIN_JWT_SECRET_LENGTH = 32;

    private ProductionSecretGuard() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** 未配置或仍为模板占位符（your-* / change-me*） */
    public static boolean isBlankOrPlaceholder(String value) {
        if (!hasText(value)) {
            return true;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith("your-") || trimmed.startsWith("change-me");
    }

    public static boolean isWeakMachineToken(String value) {
        if (isBlankOrPlaceholder(value)) {
            return true;
        }
        return value.trim().length() < MIN_MACHINE_TOKEN_LENGTH;
    }

    public static boolean isWeakAdminPassword(String value) {
        if (!hasText(value)) {
            return true;
        }
        String trimmed = value.trim();
        return DEFAULT_ADMIN_PASSWORD.equals(trimmed) || isBlankOrPlaceholder(trimmed);
    }

    public static boolean isDefaultJwtSecret(String value) {
        if (!hasText(value)) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.contains(DEFAULT_JWT_MARKER) || trimmed.length() < MIN_JWT_SECRET_LENGTH;
    }

    /** OAuth/OIDC client_secret（SSO 启用时生产须更换） */
    public static boolean isWeakOAuthClientSecret(String value) {
        return isBlankOrPlaceholder(value);
    }
}
