package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.AgentProfileProbeProperties;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProbeAlertRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentProbeAlertServiceTest {

    @Mock
    private LlmAgentProbeAlertRepo probeAlertRepo;

    private AgentProbeAlertService alertService;

    @BeforeEach
    void setUp() {
        AgentProfileProbeProperties properties = new AgentProfileProbeProperties();
        properties.setAlertWebhookUrl("");
        alertService = new AgentProbeAlertService(
                properties,
                probeAlertRepo,
                new ObjectMapper(),
                RestClient.builder());
    }

    @Test
    void notifyIfNeeded_skipsWhenWebhookUrlEmpty() {
        AgentProfileProbeResultVO result = AgentProfileProbeResultVO.builder()
                .overallStatus("UNAVAILABLE")
                .taskCode("aiChat")
                .build();

        alertService.notifyIfNeeded(1L, result);

        verify(probeAlertRepo, never()).insert(any());
    }

    @Test
    void notifyIfNeeded_skipsReadyStatus() {
        AgentProfileProbeProperties properties = new AgentProfileProbeProperties();
        properties.setAlertWebhookUrl("http://localhost:9999/webhook");
        alertService = new AgentProbeAlertService(
                properties,
                probeAlertRepo,
                new ObjectMapper(),
                RestClient.builder());

        AgentProfileProbeResultVO result = AgentProfileProbeResultVO.builder()
                .overallStatus("READY")
                .taskCode("aiChat")
                .build();

        alertService.notifyIfNeeded(1L, result);

        verify(probeAlertRepo, never()).insert(any());
    }
}
