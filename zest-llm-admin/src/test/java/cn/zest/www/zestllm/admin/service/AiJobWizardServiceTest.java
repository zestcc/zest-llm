package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.request.AiJobWizardRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.AiJobWizardResultVO;
import cn.zest.www.zestllm.admin.model.vo.ApplyScenarioTemplateResultVO;
import cn.zest.www.zestllm.admin.model.vo.ScenarioTemplateVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobWizardServiceTest {

    @Mock
    private ScenarioTemplateService scenarioTemplateService;
    @Mock
    private AgentProfileProbeService agentProfileProbeService;

    @InjectMocks
    private AiJobWizardService service;

    @Test
    void run_appliesTemplateAndProbes() {
        when(scenarioTemplateService.getTemplate("chat-basic")).thenReturn(
                ScenarioTemplateVO.builder().id("chat-basic").name("对话").taskCodeSuggestion("aiChat")
                        .recommendedTier("small").build());
        when(scenarioTemplateService.apply(any())).thenReturn(ApplyScenarioTemplateResultVO.builder()
                .taskCode("aiChat").profileVersion("v1").published(false).message("ok").build());
        when(agentProfileProbeService.probeVersion(any(), any(), any())).thenReturn(
                AgentProfileProbeResultVO.builder().overallStatus("READY").build());

        AiJobWizardRequest req = new AiJobWizardRequest();
        req.setTemplateId("chat-basic");
        req.setAppKey("order-service");
        req.setRunProbe(true);

        AiJobWizardResultVO result = service.run(req);
        assertEquals("aiChat", result.getTaskCode());
        assertEquals("READY", result.getProbeStatus());
    }
}
