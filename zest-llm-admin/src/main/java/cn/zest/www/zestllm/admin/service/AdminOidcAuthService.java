package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.AdminOidcProperties;
import cn.zest.www.zestllm.admin.config.JwtProperties;
import cn.zest.www.zestllm.admin.config.JwtTokenProvider;
import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAdminUserDO;
import cn.zest.www.zestllm.admin.model.request.AdminOidcCallbackRequest;
import cn.zest.www.zestllm.admin.model.request.AdminOidcExchangeRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcAuthorizeVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcConfigVO;
import cn.zest.www.zestllm.admin.repo.LlmAdminUserRepo;
import cn.zest.www.zestllm.admin.service.auth.OidcJwtValidator;
import cn.zest.www.zestllm.admin.service.sso.AdminOidcPkceStore;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOidcAuthService {

    private static final String SSO_PROVIDER = "zest-sso";

    private final AdminOidcProperties oidcProperties;
    private final OidcJwtValidator oidcJwtValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final LlmAdminUserRepo adminUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final AdminOidcPkceStore pkceStore;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public AdminOidcConfigVO getPublicConfig() {
        AdminOidcConfigVO vo = new AdminOidcConfigVO();
        vo.setEnabled(oidcProperties.isEnabled());
        vo.setClientId(oidcProperties.getClientId());
        vo.setIssuer(oidcProperties.getIssuer());
        return vo;
    }

    public String buildLogoutUrl() {
        if (!oidcProperties.isEnabled()) {
            return null;
        }
        String redirect = oidcProperties.getPostLogoutRedirectUri();
        return oidcProperties.getIssuer().replaceAll("/$", "")
                + "/connect/logout?post_logout_redirect_uri=" + encode(redirect);
    }

    public AdminOidcAuthorizeVO buildAuthorizeUrl() {
        if (!oidcProperties.isEnabled()) {
            throw BusinessException.badRequest("Admin OIDC SSO 未启用");
        }
        String state = randomBase64(16);
        String codeVerifier = randomBase64(32);
        String codeChallenge = sha256Base64Url(codeVerifier);
        pkceStore.save(state, codeVerifier);

        String scopes = String.join(" ", oidcProperties.getScopes());
        String url = oidcProperties.getIssuer().replaceAll("/$", "") + "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + encode(oidcProperties.getClientId())
                + "&redirect_uri=" + encode(oidcProperties.getRedirectUri())
                + "&scope=" + encode(scopes)
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";

        AdminOidcAuthorizeVO vo = new AdminOidcAuthorizeVO();
        vo.setAuthorizationUrl(url);
        vo.setState(state);
        return vo;
    }

    public AdminLoginVO handleCallback(AdminOidcCallbackRequest request) {
        if (!oidcProperties.isEnabled()) {
            throw BusinessException.badRequest("Admin OIDC SSO 未启用");
        }
        String codeVerifier = pkceStore.consume(request.getState());
        if (!StringUtils.hasText(codeVerifier)) {
            throw BusinessException.unauthorized("无效的 state 或已过期");
        }
        String idToken = exchangeCodeForIdToken(request.getCode(), codeVerifier);
        return exchangeIdToken(new AdminOidcExchangeRequest(idToken));
    }

    public AdminLoginVO exchangeIdToken(AdminOidcExchangeRequest request) {
        if (!oidcProperties.isEnabled()) {
            throw BusinessException.badRequest("Admin OIDC SSO 未启用");
        }
        if (!StringUtils.hasText(request.getIdToken())) {
            throw BusinessException.badRequest("idToken 不能为空");
        }
        InboundAuthConfig authConfig = new InboundAuthConfig();
        authConfig.setMode("OIDC_JWT");
        authConfig.setIssuer(oidcProperties.getIssuer());
        authConfig.setAudience(oidcProperties.getAudience());
        authConfig.setJwksUri(oidcProperties.getJwksUri());

        Claims claims = oidcJwtValidator.parseAndValidate(request.getIdToken(), authConfig);
        String subject = claims.getSubject();
        String username = readClaim(claims, oidcProperties.getUsernameClaim());
        if (!StringUtils.hasText(username)) {
            username = subject;
        }
        String role = mapRole(claims);

        LlmAdminUserDO user = provisionUser(subject, username, role, readClaim(claims, "name"));

        String token = jwtTokenProvider.createToken(user.getUsername(), user.getRole());
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtProperties.getExpirationMs() / 1000);
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        return vo;
    }

    private LlmAdminUserDO provisionUser(String subject, String username, String role, String displayName) {
        return adminUserRepo.findBySsoSubject(SSO_PROVIDER, subject)
                .or(() -> adminUserRepo.findByUsername(username))
                .map(existing -> {
                    existing.setSsoProvider(SSO_PROVIDER);
                    existing.setSsoSubject(subject);
                    existing.setRole(role);
                    if (StringUtils.hasText(displayName)) {
                        existing.setDisplayName(displayName);
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    adminUserRepo.update(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    LlmAdminUserDO user = new LlmAdminUserDO();
                    user.setUsername(username);
                    user.setSsoProvider(SSO_PROVIDER);
                    user.setSsoSubject(subject);
                    user.setDisplayName(StringUtils.hasText(displayName) ? displayName : username);
                    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setStatus("ACTIVE");
                    user.setRole(role);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    adminUserRepo.insert(user);
                    return user;
                });
    }

    private String mapRole(Claims claims) {
        Object roles = claims.get(oidcProperties.getRolesClaim());
        if (roles instanceof List<?> list) {
            for (Object r : list) {
                String code = String.valueOf(r);
                if ("SSO_ADMIN".equals(code) || "ADMIN".equals(code)) {
                    return "ADMIN";
                }
                if ("SSO_OPERATOR".equals(code) || "OPERATOR".equals(code)) {
                    return "OPERATOR";
                }
            }
        }
        return oidcProperties.getDefaultRole();
    }

    private String exchangeCodeForIdToken(String code, String codeVerifier) {
        String tokenEndpoint = oidcProperties.getIssuer().replaceAll("/$", "") + "/oauth2/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", oidcProperties.getRedirectUri());
        form.add("client_id", oidcProperties.getClientId());
        form.add("client_secret", oidcProperties.getClientSecret());
        form.add("code_verifier", codeVerifier);
        try {
            String body = restClientBuilder.build()
                    .post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode json = objectMapper.readTree(body);
            String idToken = json.path("id_token").asText(null);
            if (!StringUtils.hasText(idToken)) {
                throw BusinessException.unauthorized("SSO Token 交换失败");
            }
            return idToken;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("SSO token exchange failed", ex);
            throw BusinessException.unauthorized("SSO Token 交换失败");
        }
    }

    private String readClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value != null ? String.valueOf(value) : null;
    }

    private String randomBase64(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String sha256Base64Url(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
