package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentProfilePublishServiceTest {

    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmAgentProfileRepo agentProfileRepo;
    @Mock
    private LlmAppRepo appRepo;
    @Mock
    private PolicyCacheAdapter policyCacheAdapter;
    @Mock
    private ResponseCacheAdapter responseCacheAdapter;
    @Mock
    private AuditService auditService;
    @Mock
    private AgentProfileResolver agentProfileResolver;
    @Mock
    private ProfileExtensionsValidator profileExtensionsValidator;
    @Mock
    private ZestEvalLearningPipelineAdapter learningPipelineAdapter;
    @Mock
    private AgentProfileProbeService agentProfileProbeService;

    @InjectMocks
    private AgentProfilePublishService publishService;

    @Test
    void publish_withoutLearningLoop_blocksWhenProbeNotReady() {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(1L);
        task.setCode("aiChat");
        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setProfileJson("{\"apiVersion\":\"zestllm/v1\",\"runtimeMode\":\"agent\"}");

        AgentProfileDocument doc = new AgentProfileDocument();
        doc.setRuntimeMode("agent");

        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));
        when(agentProfileRepo.findByTaskIdAndVersion(1L, "v1")).thenReturn(Optional.of(profile));
        when(agentProfileResolver.parseProfile(profile.getProfileJson(), null)).thenReturn(doc);
        when(agentProfileProbeService.probeVersion(eq("aiChat"), eq("v1"), any(AgentProfileProbeRequest.class)))
                .thenReturn(AgentProfileProbeResultVO.builder().ready(false).overallStatus("DEGRADED").build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> publishService.publish("aiChat", "v1", "admin"));
        assertEquals("PROBE_FAILED", ex.getErrorCode());
        verify(learningPipelineAdapter, never()).validateForPublish(any(), any(), any());
    }
}
