package cn.zest.www.zestllm.admin.service.sso.spi;

import cn.zest.www.zestllm.admin.config.AdminSsoProperties;
import cn.zest.www.zestllm.admin.service.sso.provider.DisabledAdminSsoProvider;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 按配置选择 SSO 提供方：enabled=false 或 provider=none 时使用 {@link DisabledAdminSsoProvider}。
 */
@Component
public class AdminSsoProviderRegistry {

    private final AdminSsoProperties properties;
    private final Map<String, AdminSsoProvider> providers;
    private final DisabledAdminSsoProvider disabledAdminSsoProvider;

    public AdminSsoProviderRegistry(AdminSsoProperties properties,
                                    List<AdminSsoProvider> providerList,
                                    DisabledAdminSsoProvider disabledAdminSsoProvider) {
        this.properties = properties;
        this.disabledAdminSsoProvider = disabledAdminSsoProvider;
        this.providers = providerList.stream()
                .filter(p -> !(p instanceof DisabledAdminSsoProvider))
                .collect(Collectors.toMap(AdminSsoProvider::providerId, Function.identity(), (a, b) -> a));
    }

    public AdminSsoProvider resolve() {
        if (!properties.isEnabled() || isDisabledProviderId(properties.getProvider())) {
            return disabledAdminSsoProvider;
        }
        AdminSsoProvider provider = providers.get(normalize(properties.getProvider()));
        return provider != null ? provider : disabledAdminSsoProvider;
    }

    public AdminSsoProperties properties() {
        return properties;
    }

    private static boolean isDisabledProviderId(String provider) {
        return !StringUtils.hasText(provider) || "none".equalsIgnoreCase(provider.trim());
    }

    private static String normalize(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase();
    }
}
