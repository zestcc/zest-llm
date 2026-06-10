package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.AgentProfileProbeProperties;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmMcpServerRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.admin.repo.LlmProviderPresetRepo;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentProfileProbeServiceTest {

    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmAppRepo appRepo;
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
    private AgentProfileResolver agentProfileResolver;
    @Mock
    private SecretResolver secretResolver;
    @Mock
    private McpToolAdapter mcpToolAdapter;
    @Mock
    private AgentProfileProbeRecordService probeRecordService;
    @Mock
    private AgentProfileProbeProperties probeProperties;
    @Mock
    private AgentRuntimeAdapter agentRuntimeAdapter;
    @Mock
    private KnowledgeRetrievalAdapter knowledgeRetrievalAdapter;

    private AgentProfileProbeService probeService;

    @BeforeEach
    void setUp() {
        when(probeProperties.getMaxParallel()).thenReturn(4);
        when(probeProperties.getBatchTimeoutSeconds()).thenReturn(120);
        when(agentRuntimeAdapter.health()).thenReturn(HealthStatus.builder().up(true).message("ok").build());
        when(knowledgeRetrievalAdapter.health()).thenReturn(HealthStatus.builder().up(true).message("ok").build());
        when(knowledgeRetrievalAdapter.retrieve(any())).thenReturn(KnowledgeRetrieveResult.builder().build());
        probeService = new AgentProfileProbeService(
                taskDefRepo,
                appRepo,
                agentProfileRepo,
                promptVersionRepo,
                modelRouteRepo,
                providerPresetRepo,
                mcpServerRepo,
                agentProfileResolver,
                secretResolver,
                mcpToolAdapter,
                new ObjectMapper(),
                probeRecordService,
                probeProperties,
                agentRuntimeAdapter,
                knowledgeRetrievalAdapter);
    }

    @Test
    void probePublished_marksUnavailableWhenGatewayDown() {
        stubTaskAndApp();
        stubPublishedProfile();
        stubPromptAndRoute();
        AgentProfileDocument document = new AgentProfileDocument();
        document.setApiVersion(AgentProfileDocument.API_VERSION);
        document.setProviderRef("litellm-default");
        when(agentProfileResolver.parseProfile(any(), isNull())).thenReturn(document);
        when(agentProfileResolver.resolve(any(), any(), any(), any())).thenReturn(CachedPolicy.builder()
                .primaryModel("gpt-4o-mini")
                .gatewayBaseUrl("http://127.0.0.1:59999")
                .build());
        when(probeRecordService.save(any(), any(), any(Boolean.class), any())).thenAnswer(inv -> inv.getArgument(1));

        AgentProfileProbeResultVO result = probeService.probePublished("aiChat", new AgentProfileProbeRequest());

        assertEquals("UNAVAILABLE", result.getOverallStatus());
        assertFalse(result.isReady());
        assertTrue(result.getChecks().stream()
                .anyMatch(c -> "gateway-health".equals(c.getName()) && !c.isUp()));
    }

    @Test
    void probeVersion_reportsMissingPromptAsUnavailable() {
        stubTaskAndApp();
        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setVersion("v2");
        profile.setStatus("DRAFT");
        profile.setProfileJson("""
                {"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-default",
                "model":{"primary":"gpt-4o-mini"},"generation":{"maxTokens":128}}
                """);
        when(agentProfileRepo.findByTaskIdAndVersion(1L, "v2")).thenReturn(Optional.of(profile));
        when(promptVersionRepo.findPublishedByTaskId(1L)).thenReturn(Optional.empty());
        when(modelRouteRepo.findActiveByTaskId(1L)).thenReturn(Optional.of(new LlmModelRouteDO()));
        when(probeRecordService.save(any(), any(), any(Boolean.class), any())).thenAnswer(inv -> inv.getArgument(1));

        AgentProfileProbeResultVO result = probeService.probeVersion("aiChat", "v2", new AgentProfileProbeRequest());

        assertEquals("UNAVAILABLE", result.getOverallStatus());
        assertTrue(result.getChecks().stream()
                .anyMatch(c -> "prompt-published".equals(c.getName()) && !c.isUp()));
    }

    private void stubTaskAndApp() {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(1L);
        task.setAppId(10L);
        task.setCode("aiChat");
        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));

        LlmAppDO app = new LlmAppDO();
        app.setId(10L);
        app.setAppKey("order-service");
        when(appRepo.findById(10L)).thenReturn(Optional.of(app));
    }

    private void stubPublishedProfile() {
        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setVersion("v1");
        profile.setStatus("PUBLISHED");
        profile.setProviderPresetCode("litellm-default");
        profile.setProfileJson("""
                {"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-default",
                "model":{"primary":"gpt-4o-mini"},"generation":{"maxTokens":128}}
                """);
        when(agentProfileRepo.findPublishedByTaskId(1L)).thenReturn(Optional.of(profile));
    }

    private void stubPromptAndRoute() {
        LlmPromptVersionDO prompt = new LlmPromptVersionDO();
        prompt.setVersion("v1");
        when(promptVersionRepo.findPublishedByTaskId(1L)).thenReturn(Optional.of(prompt));

        LlmModelRouteDO route = new LlmModelRouteDO();
        route.setPrimaryModel("gpt-4o-mini");
        when(modelRouteRepo.findActiveByTaskId(1L)).thenReturn(Optional.of(route));
    }
}
