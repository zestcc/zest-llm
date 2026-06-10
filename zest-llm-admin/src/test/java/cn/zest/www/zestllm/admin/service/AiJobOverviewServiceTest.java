package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileProbeDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.vo.AiJobOverviewVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileProbeRepo;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobOverviewServiceTest {

    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmAppRepo appRepo;
    @Mock
    private LlmAgentProfileRepo agentProfileRepo;
    @Mock
    private LlmAgentProfileProbeRepo probeRepo;
    @Mock
    private LlmExecutionRepo executionRepo;

    @InjectMocks
    private AiJobOverviewService service;

    @Test
    void listOverview_aggregatesTaskMetrics() {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(1L);
        task.setAppId(10L);
        task.setCode("aiChat");
        task.setName("对话");
        task.setStatus("ACTIVE");

        LlmAppDO app = new LlmAppDO();
        app.setAppKey("order-service");

        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setVersion("v1");

        LlmAgentProfileProbeDO probe = new LlmAgentProfileProbeDO();
        probe.setOverallStatus("READY");
        probe.setCreatedAt(LocalDateTime.now());

        when(taskDefRepo.findAll()).thenReturn(List.of(task));
        when(appRepo.findById(10L)).thenReturn(Optional.of(app));
        when(agentProfileRepo.findPublishedByTaskId(1L)).thenReturn(Optional.of(profile));
        when(probeRepo.findLatestByTaskId(1L)).thenReturn(Optional.of(probe));
        when(executionRepo.countByTaskCodeSince(eq("aiChat"), any())).thenReturn(42L);
        when(executionRepo.countByTaskCodeAndStatusSince(eq("aiChat"), eq("FAILED"), any())).thenReturn(2L);

        List<AiJobOverviewVO> overview = service.listOverview();
        assertEquals(1, overview.size());
        assertEquals("v1", overview.get(0).getPublishedVersion());
        assertEquals(42L, overview.get(0).getExecutionsLast7d());
        assertEquals(2L, overview.get(0).getFailedLast7d());
    }
}
