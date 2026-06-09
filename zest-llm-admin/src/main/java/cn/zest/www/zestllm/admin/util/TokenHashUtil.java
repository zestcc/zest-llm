package cn.zest.www.zestllm.admin.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

public final class TokenHashUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;

    private TokenHashUtil() {
    }

    public static String generateRawToken() {
        return "zllm_" + UUID.randomUUID();
    }

    public static String encodeToken(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        String hashHex = sha256Hex(saltHex + raw);
        return saltHex + "." + hashHex;
    }

    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        int dot = stored.indexOf('.');
        if (dot > 0 && dot < stored.length() - 1) {
            String saltHex = stored.substring(0, dot);
            String expectedHash = stored.substring(dot + 1);
            return sha256Hex(saltHex + raw).equals(expectedHash);
        }
        return sha256Hex(raw).equals(stored);
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public static String newTraceId() {
        return "tr_" + UUID.randomUUID().toString().replace("-", "");
    }
}
