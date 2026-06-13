package cn.zest.www.zestllm.admin.service.sso.oidc;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProviderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OIDC Discovery 或静态配置解析端点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OidcEndpointResolver {

    private static final Duration DISCOVERY_CACHE_TTL = Duration.ofHours(1);

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedDiscovery> discoveryCache = new ConcurrentHashMap<>();

    public OidcEndpoints resolve(AdminSsoProviderConfig config) {
        if (StringUtils.hasText(config.getDiscoveryUri())) {
            return resolveFromDiscovery(config.getDiscoveryUri());
        }
        return resolveFromStatic(config);
    }

    private OidcEndpoints resolveFromDiscovery(String discoveryUri) {
        CachedDiscovery cached = discoveryCache.get(discoveryUri);
        if (cached == null || cached.expiresAt().isBefore(Instant.now())) {
            cached = loadDiscovery(discoveryUri);
            discoveryCache.put(discoveryUri, cached);
        }
        return cached.endpoints();
    }

    private CachedDiscovery loadDiscovery(String discoveryUri) {
        try {
            String body = restClientBuilder.build()
                    .get()
                    .uri(discoveryUri)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            String issuer = root.path("issuer").asText(null);
            if (!StringUtils.hasText(issuer)) {
                throw BusinessException.badRequest("OIDC Discovery 缺少 issuer");
            }
            OidcEndpoints endpoints = new OidcEndpoints(
                    issuer,
                    requiredText(root, "authorization_endpoint"),
                    requiredText(root, "token_endpoint"),
                    requiredText(root, "jwks_uri"),
                    root.path("end_session_endpoint").asText(null)
            );
            return new CachedDiscovery(endpoints, Instant.now().plus(DISCOVERY_CACHE_TTL));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("OIDC Discovery 加载失败: {}", discoveryUri, ex);
            throw BusinessException.badRequest("OIDC Discovery 加载失败");
        }
    }

    private OidcEndpoints resolveFromStatic(AdminSsoProviderConfig config) {
        String issuer = config.getIssuer().replaceAll("/$", "");
        String jwksUri = StringUtils.hasText(config.getJwksUri()) ? config.getJwksUri() : issuer + "/oauth2/jwks";
        return new OidcEndpoints(
                issuer,
                issuer + "/oauth2/authorize",
                issuer + "/oauth2/token",
                jwksUri,
                issuer + "/connect/logout"
        );
    }

    private static String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest("OIDC Discovery 缺少 " + field);
        }
        return value;
    }

    private record CachedDiscovery(OidcEndpoints endpoints, Instant expiresAt) {
    }
}
