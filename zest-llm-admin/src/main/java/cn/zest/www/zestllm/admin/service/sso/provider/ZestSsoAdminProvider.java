package cn.zest.www.zestllm.admin.service.sso.provider;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.service.auth.OidcJwtValidator;
import cn.zest.www.zestllm.admin.service.sso.AdminSsoUserProvisioner;
import cn.zest.www.zestllm.admin.service.sso.oidc.OidcEndpointResolver;
import cn.zest.www.zestllm.admin.service.sso.oidc.OidcTokenClient;
import cn.zest.www.zestllm.admin.service.sso.oidc.PkceUtils;
import cn.zest.www.zestllm.admin.service.sso.store.AdminSsoPkceStore;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProviderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * ZestSSO 提供方 — 默认 Discovery + logout-url API。
 */
@Slf4j
@Component
public class ZestSsoAdminProvider extends AbstractOidcAdminProvider {

    public static final String PROVIDER_ID = "zest-sso";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final cn.zest.www.zestllm.admin.config.AdminSsoProperties ssoProperties;

    public ZestSsoAdminProvider(AdminSsoPkceStore pkceStore,
                                OidcEndpointResolver endpointResolver,
                                OidcTokenClient tokenClient,
                                OidcJwtValidator jwtValidator,
                                AdminSsoUserProvisioner userProvisioner,
                                RestClient.Builder restClientBuilder,
                                ObjectMapper objectMapper,
                                cn.zest.www.zestllm.admin.config.AdminSsoProperties ssoProperties) {
        super(pkceStore, endpointResolver, tokenClient, jwtValidator, userProvisioner);
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.ssoProperties = ssoProperties;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    protected String displayName() {
        return StringUtils.hasText(ssoProperties.getDisplayName()) ? ssoProperties.getDisplayName() : "ZestSSO";
    }

    @Override
    public String buildLogoutUrl(AdminSsoProviderConfig config) {
        if (config.isZestSsoUseLogoutUrlApi()) {
            return fetchLogoutUrlFromApi(config, config.getZestSsoLogoutUrlApiPath());
        }
        return super.buildLogoutUrl(config);
    }

    private String fetchLogoutUrlFromApi(AdminSsoProviderConfig config, String apiPath) {
        String base = config.getIssuer().replaceAll("/$", "");
        String path = StringUtils.hasText(apiPath) ? apiPath : "/api/public/logout-url";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String redirect = config.getPostLogoutRedirectUri();
        try {
            String body = restClientBuilder.build()
                    .get()
                    .uri(base + path + "?redirect_uri=" + PkceUtils.urlEncode(redirect))
                    .retrieve()
                    .body(String.class);
            return parseLogoutUrlResponse(body, objectMapper);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ZestSSO logout-url 调用异常", ex);
            throw BusinessException.badRequest("SSO 登出 URL 获取失败");
        }
    }

    /** 解析 ZestSSO ApiResponse&lt;String&gt; 登出 URL */
    static String parseLogoutUrlResponse(String body, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("code").asInt(-1) != 0) {
                throw BusinessException.badRequest("SSO 登出 URL 获取失败");
            }
            String url = root.path("data").asText(null);
            if (!StringUtils.hasText(url)) {
                throw BusinessException.badRequest("SSO 登出 URL 获取失败");
            }
            return url;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest("SSO 登出 URL 获取失败");
        }
    }
}
