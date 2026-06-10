package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.admin.repo.LlmMcpServerRepo;
import cn.zest.www.zestllm.admin.repo.LlmProviderPresetRepo;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentProfileResolverTest {

    @Mock
    private LlmAgentProfileRepo agentProfileRepo;
    @Mock
    private LlmPromptVersionRepo promptVersionRepo;
    @Mock
    private LlmModelRouteRepo modelRouteRepo;
    @Mock
    private LlmProviderPresetRepo providerPresetRepo;
    @Mock
    private LlmMcpServerRepo mcpServerRepo;
    @Mock
    private RuntimeAuthService runtimeAuthService;

    private AgentProfileResolver agentProfileResolver;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        agentProfileResolver = new AgentProfileResolver(
                agentProfileRepo,
                promptVersionRepo,
                modelRouteRepo,
                providerPresetRepo,
                mcpServerRepo,
                runtimeAuthService,
                new ObjectMapper());
    }

    @Test
    void resolve_mergesLegacyPromptAndRoute() {
        LlmAppDO app = new LlmAppDO();
        app.setId(1L);
        app.setAppKey("order-service");
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(1L);
        task.setCode("aiChat");

        LlmPromptVersionDO prompt = new LlmPromptVersionDO();
        prompt.setVersion("v1");
        prompt.setTemplateBody("hello {{question}}");
        prompt.setOutputSchema("{}");

        LlmModelRouteDO route = new LlmModelRouteDO();
        route.setPrimaryModel("gpt-4o-mini");
        route.setFallbackModels("gpt-3.5-turbo");
        route.setMaxTokens(1024);
        route.setTemperature(new BigDecimal("0.70"));
        route.setTimeoutMs(30000);

        when(agentProfileRepo.findPublishedByTaskId(1L)).thenReturn(Optional.empty());
        when(promptVersionRepo.findPublishedByTaskId(1L)).thenReturn(Optional.of(prompt));
        when(modelRouteRepo.findActiveByTaskId(1L)).thenReturn(Optional.of(route));
        InboundAuthConfig inbound = new InboundAuthConfig();
        inbound.setMode("STATIC_TOKEN");
        when(runtimeAuthService.resolveInboundAuth(any())).thenReturn(inbound);

        CachedPolicy policy = agentProfileResolver.resolve(app, task, "tr_test");

        assertEquals("v1", policy.getPromptVersion());
        assertEquals("gpt-4o-mini", policy.getPrimaryModel());
        assertEquals(1024, policy.getMaxTokens());
        assertNotNull(policy.getTemplateBody());
    }
}
