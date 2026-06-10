package cn.zest.www.zestllm.admin.service.auth;

import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OidcJwtValidator {

    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(1);

    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final Map<String, CachedJwks> jwksCache = new ConcurrentHashMap<>();

    public void validate(String jwt, InboundAuthConfig config) {
        parseAndValidate(jwt, config);
    }

    public Claims parseAndValidate(String jwt, InboundAuthConfig config) {
        if (!StringUtils.hasText(jwt)) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        String kid = readKid(parts[0]);
        PublicKey publicKey = resolvePublicKey(config, kid);
        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(jwt);
        } catch (ExpiredJwtException ex) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Token expired");
        } catch (Exception ex) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Invalid JWT");
        }
        Claims claims = jws.getPayload();
        if (claims.getExpiration() != null && claims.getExpiration().toInstant().isBefore(Instant.now())) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Token expired");
        }
        if (StringUtils.hasText(config.getIssuer()) && !config.getIssuer().equals(claims.getIssuer())) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Invalid issuer");
        }
        if (StringUtils.hasText(config.getAudience())) {
            Object aud = claims.get("aud");
            if (aud instanceof String audStr && !config.getAudience().equals(audStr)) {
                throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Invalid audience");
            }
            if (aud instanceof java.util.List<?> audList && !audList.contains(config.getAudience())) {
                throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Invalid audience");
            }
        }
        return claims;
    }

    private String readKid(String headerPart) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(headerPart);
            JsonNode header = objectMapper.readTree(decoded);
            return header.path("kid").asText(null);
        } catch (Exception ex) {
            log.debug("Failed to parse JWT header kid", ex);
            return null;
        }
    }

    private PublicKey resolvePublicKey(InboundAuthConfig config, String kid) {
        String jwksUri = config.getJwksUri();
        if (!StringUtils.hasText(jwksUri) && StringUtils.hasText(config.getIssuer())) {
            jwksUri = config.getIssuer().replaceAll("/$", "") + "/.well-known/jwks.json";
        }
        if (!StringUtils.hasText(jwksUri)) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "JWKS URI not configured");
        }
        CachedJwks cached = jwksCache.get(jwksUri);
        if (cached == null || cached.expiresAt().isBefore(Instant.now())) {
            cached = loadJwks(jwksUri);
            jwksCache.put(jwksUri, cached);
        }
        JsonNode keyNode = findKey(cached.keys(), kid);
        if (keyNode == null) {
            cached = loadJwks(jwksUri);
            jwksCache.put(jwksUri, cached);
            keyNode = findKey(cached.keys(), kid);
        }
        if (keyNode == null) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Signing key not found");
        }
        return toPublicKey(keyNode);
    }

    private CachedJwks loadJwks(String jwksUri) {
        try {
            String body = restClientBuilder.build()
                    .get()
                    .uri(jwksUri)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            return new CachedJwks(root.path("keys"), Instant.now().plus(JWKS_CACHE_TTL));
        } catch (Exception ex) {
            log.warn("Failed to load JWKS from {}", jwksUri, ex);
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "JWKS fetch failed");
        }
    }

    private JsonNode findKey(JsonNode keys, String kid) {
        if (keys == null || !keys.isArray()) {
            return null;
        }
        for (JsonNode key : keys) {
            if (!StringUtils.hasText(kid) || kid.equals(key.path("kid").asText(null))) {
                if ("RSA".equals(key.path("kty").asText())) {
                    return key;
                }
            }
        }
        return keys.isArray() && keys.size() > 0 ? keys.get(0) : null;
    }

    private PublicKey toPublicKey(JsonNode keyNode) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(keyNode.path("n").asText());
            byte[] eBytes = Base64.getUrlDecoder().decode(keyNode.path("e").asText());
            RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED, "Invalid JWKS key");
        }
    }

    private record CachedJwks(JsonNode keys, Instant expiresAt) {
    }
}
