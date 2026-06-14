package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.AdapterCatalogDetailVO;
import cn.zest.www.zestllm.admin.model.vo.AdapterCatalogPageVO;
import cn.zest.www.zestllm.admin.plugin.AdapterCatalogDefinitions;
import cn.zest.www.zestllm.admin.plugin.AdapterEnablementChecker;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdapterCatalogServiceTest {

    @Mock
    private AdapterEnablementChecker enablementChecker;
    @Mock
    private AdapterConfigService adapterConfigService;
    @Mock
    private AdapterHealthService adapterHealthService;
    @Mock
    private cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry externalAdapterRegistry;
    @Mock
    private cn.zest.www.zestllm.infra.config.LlmPluginProperties pluginProperties;

    private LlmAdapterProperties adapterProperties;
    private Environment environment;

    @InjectMocks
    private AdapterCatalogService adapterCatalogService;

    @BeforeEach
    void setUp() {
        adapterProperties = new LlmAdapterProperties();
        environment = new MockEnvironment();
        adapterCatalogService = new AdapterCatalogService(
                enablementChecker, adapterConfigService, adapterHealthService,
                adapterProperties, environment, externalAdapterRegistry, pluginProperties);
    }

    @Test
    void catalogShouldIncludeLitellm() {
        when(enablementChecker.resolveActivePluginId("model-gateway")).thenReturn("litellm");
        when(enablementChecker.isActive("model-gateway", "litellm")).thenReturn(true);
        AdapterCatalogPageVO page = adapterCatalogService.catalog(null);
        assertThat(page.getPlugins()).isNotEmpty();
        assertThat(page.getPlugins().stream().anyMatch(p ->
                "model-gateway:litellm".equals(p.getCatalogKey()))).isTrue();
    }

    @Test
    void detailShouldExposeIntegrationSteps() {
        when(enablementChecker.resolveActivePluginId("model-gateway")).thenReturn("litellm");
        when(enablementChecker.isActive("model-gateway", "litellm")).thenReturn(true);
        AdapterCatalogDetailVO detail = adapterCatalogService.detail("model-gateway:litellm");
        assertThat(detail.getPluginId()).isEqualTo("litellm");
        assertThat(detail.getIntegrationSteps()).isNotEmpty();
        assertThat(detail.getIntegrationSteps().get(0).getTitle()).isNotBlank();
    }

    @Test
    void definitionsShouldHaveUniqueCatalogKeys() {
        long distinct = AdapterCatalogDefinitions.all().stream().map(e -> e.catalogKey()).distinct().count();
        assertThat(distinct).isEqualTo(AdapterCatalogDefinitions.all().size());
    }
}
