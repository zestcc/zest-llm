package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.AdminOidcProperties;
import cn.zest.www.zestllm.admin.config.JwtProperties;
import cn.zest.www.zestllm.admin.config.JwtTokenProvider;
import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.request.AdminOidcExchangeRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcConfigVO;
import cn.zest.www.zestllm.admin.service.auth.OidcJwtValidator;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminOidcAuthService {

    private final AdminOidcProperties oidcProperties;
    private final OidcJwtValidator oidcJwtValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AdminOidcConfigVO getPublicConfig() {
        AdminOidcConfigVO vo = new AdminOidcConfigVO();
        vo.setEnabled(oidcProperties.isEnabled());
        vo.setClientId(oidcProperties.getClientId());
        vo.setIssuer(oidcProperties.getIssuer());
        return vo;
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
        String username = readClaim(claims, oidcProperties.getUsernameClaim());
        if (!StringUtils.hasText(username)) {
            username = readClaim(claims, "sub");
        }
        if (!StringUtils.hasText(username)) {
            throw BusinessException.unauthorized("OIDC token 缺少用户名 claim");
        }

        String token = jwtTokenProvider.createToken(username, oidcProperties.getDefaultRole());
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtProperties.getExpirationMs() / 1000);
        vo.setUsername(username);
        vo.setRole(oidcProperties.getDefaultRole());
        return vo;
    }

    private String readClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value != null ? String.valueOf(value) : null;
    }
}
