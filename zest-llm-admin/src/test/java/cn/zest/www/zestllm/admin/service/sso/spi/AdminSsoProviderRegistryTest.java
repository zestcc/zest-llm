package cn.zest.www.zestllm.admin.service.sso.spi;

import cn.zest.www.zestllm.admin.config.AdminSsoProperties;
import cn.zest.www.zestllm.admin.service.sso.provider.DisabledAdminSsoProvider;
import cn.zest.www.zestllm.admin.service.sso.provider.GenericOidcAdminProvider;
import cn.zest.www.zestllm.admin.service.sso.provider.ZestSsoAdminProvider;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSsoProviderRegistryTest {

    @Mock
    private ZestSsoAdminProvider zestSsoAdminProvider;
    @Mock
    private GenericOidcAdminProvider genericOidcAdminProvider;

    private AdminSsoProperties properties;
    private DisabledAdminSsoProvider disabledAdminSsoProvider;
    private AdminSsoProviderRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new AdminSsoProperties();
        disabledAdminSsoProvider = new DisabledAdminSsoProvider();
        when(zestSsoAdminProvider.providerId()).thenReturn(ZestSsoAdminProvider.PROVIDER_ID);
        when(genericOidcAdminProvider.providerId()).thenReturn(GenericOidcAdminProvider.PROVIDER_ID);
        registry = new AdminSsoProviderRegistry(
                properties,
                List.of(zestSsoAdminProvider, genericOidcAdminProvider, disabledAdminSsoProvider),
                disabledAdminSsoProvider);
    }

    @Test
    void resolve_whenDisabled_returnsDisabledProvider() {
        properties.setEnabled(false);
        properties.setProvider("zest-sso");

        AdminSsoProvider provider = registry.resolve();

        assertThat(provider).isSameAs(disabledAdminSsoProvider);
    }

    @Test
    void resolve_whenProviderNone_returnsDisabledProvider() {
        properties.setEnabled(true);
        properties.setProvider("none");

        AdminSsoProvider provider = registry.resolve();

        assertThat(provider).isSameAs(disabledAdminSsoProvider);
    }

    @Test
    void resolve_whenZestSsoEnabled_returnsZestSsoProvider() {
        properties.setEnabled(true);
        properties.setProvider("zest-sso");

        AdminSsoProvider provider = registry.resolve();

        assertThat(provider).isSameAs(zestSsoAdminProvider);
    }

    @Test
    void resolve_whenOidcEnabled_returnsGenericProvider() {
        properties.setEnabled(true);
        properties.setProvider("oidc");

        AdminSsoProvider provider = registry.resolve();

        assertThat(provider).isSameAs(genericOidcAdminProvider);
    }
}
