package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import cn.zest.www.zestllm.common.api.integration.AppIntegrationStatusResponse;
import cn.zest.www.zestllm.common.api.integration.AppTaskAvailabilityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppIntegrationServiceTest {

    @Mock
    private RuntimeAuthService runtimeAuthService;
    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmAgentProfileRepo agentProfileRepo;
    @Mock
    private LlmModelRouteRepo modelRouteRepo;
    @Mock
    private AgentProfileProbeService agentProfileProbeService;
    @Mock
    private AgentProfileProbeRecordService probeRecordService;

    private AppIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new AppIntegrationService(
                runtimeAuthService,
                taskDefRepo,
                agentProfileRepo,
                modelRouteRepo,
                agentProfileProbeService,
                probeRecordService);
    }

    @Test
    void getIntegrationStatus_aggregatesTaskSummaries() {
        LlmAppDO app = app("zeststory", "ZestStory");
        LlmAiTaskDefDO invoke = task(1L, app.getId(), "zestStoryInvoke");
        LlmAgentProfileDO profile = profile(invoke.getId(), "v2", "PUBLISHED", "agent", "litellm-local");
        LlmModelRouteDO route = route(invoke.getId(), "deepseek-v4-flash");
        AgentProfileProbeResultVO probe = AgentProfileProbeResultVO.builder()
                .taskCode("zestStoryInvoke")
                .overallStatus("READY")
                .ready(true)
                .probedAt(LocalDateTime.now())
                .build();

        when(runtimeAuthService.authenticate("token", "zeststory")).thenReturn(app);
        when(taskDefRepo.findByAppId(app.getId())).thenReturn(List.of(invoke));
        when(agentProfileRepo.findPublishedByTaskId(invoke.getId())).thenReturn(Optional.of(profile));
        when(modelRouteRepo.findActiveByTaskId(invoke.getId())).thenReturn(Optional.of(route));
        when(probeRecordService.latest("zestStoryInvoke")).thenReturn(Optional.of(probe));

        AppIntegrationStatusResponse status = service.getIntegrationStatus("token", "zeststory");
        assertEquals("zeststory", status.getAppKey());
        assertEquals("READY", status.getOverallStatus());
        assertTrue(status.isReady());
        assertEquals(1, status.getTaskCount());
        assertEquals(1, status.getReadyTaskCount());
        assertEquals("deepseek-v4-flash", status.getTasks().get(0).getPrimaryModel());
    }

    @Test
    void getTaskAvailability_delegatesToProbe() {
        LlmAppDO app = app("zeststory", "ZestStory");
        LlmAiTaskDefDO task = task(2L, app.getId(), "zestStoryRag");
        AgentProfileProbeResultVO probe = AgentProfileProbeResultVO.builder()
                .taskCode("zestStoryRag")
                .profileVersion("v1")
                .profileStatus("PUBLISHED")
                .overallStatus("UNAVAILABLE")
                .ready(false)
                .latencyMs(120L)
                .build();

        when(runtimeAuthService.authenticate("token", "zeststory")).thenReturn(app);
        when(taskDefRepo.findByAppIdAndCode(app.getId(), "zestStoryRag")).thenReturn(Optional.of(task));
        when(agentProfileProbeService.probePublished(eq("zestStoryRag"), any())).thenReturn(probe);

        AppTaskAvailabilityResponse availability = service.getTaskAvailability("token", "zeststory", "zestStoryRag", false);
        assertEquals("zestStoryRag", availability.getTaskCode());
        assertFalse(availability.isReady());
        assertEquals("UNAVAILABLE", availability.getOverallStatus());
    }

    private static LlmAppDO app(String key, String name) {
        LlmAppDO app = new LlmAppDO();
        app.setId(10L);
        app.setAppKey(key);
        app.setAppName(name);
        app.setStatus("ACTIVE");
        return app;
    }

    private static LlmAiTaskDefDO task(Long id, Long appId, String code) {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(id);
        task.setAppId(appId);
        task.setCode(code);
        task.setName(code);
        task.setStatus("ACTIVE");
        return task;
    }

    private static LlmAgentProfileDO profile(Long taskId, String version, String status, String runtimeMode, String preset) {
        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setTaskId(taskId);
        profile.setVersion(version);
        profile.setStatus(status);
        profile.setRuntimeMode(runtimeMode);
        profile.setProviderPresetCode(preset);
        return profile;
    }

    private static LlmModelRouteDO route(Long taskId, String model) {
        LlmModelRouteDO route = new LlmModelRouteDO();
        route.setTaskId(taskId);
        route.setPrimaryModel(model);
        route.setStatus("ACTIVE");
        return route;
    }
}
