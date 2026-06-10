package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.request.ApplyScenarioTemplateRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileVO;
import cn.zest.www.zestllm.admin.model.vo.ApplyScenarioTemplateResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioTemplateServiceTest {

    @Mock
    private AgentProfileManageService agentProfileManageService;
    @Mock
    private TaskManageService taskManageService;
    @Mock
    private LlmAiTaskDefRepo taskDefRepo;

    private ScenarioTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ScenarioTemplateService(
                agentProfileManageService, taskManageService, taskDefRepo, new ObjectMapper());
        service.loadTemplates();
    }

    @Test
    void listTemplates_loadsBundledTemplates() {
        List<?> templates = service.listTemplates();
        assertTrue(templates.size() >= 3);
    }

    @Test
    void apply_unknownTemplate_throws() {
        ApplyScenarioTemplateRequest req = new ApplyScenarioTemplateRequest();
        req.setTemplateId("missing");
        req.setAppKey("order-service");
        assertThrows(BusinessException.class, () -> service.apply(req));
    }

    @Test
    void apply_chatBasic_createsProfile() {
        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.empty());
        when(agentProfileManageService.importProfile(any())).thenReturn(
                AgentProfileVO.builder().version("v-test").build());

        ApplyScenarioTemplateRequest req = new ApplyScenarioTemplateRequest();
        req.setTemplateId("chat-basic");
        req.setAppKey("order-service");
        req.setTaskCode("aiChat");

        ApplyScenarioTemplateResultVO result = service.apply(req);
        assertEquals("aiChat", result.getTaskCode());
        verify(taskManageService).create(any());
        verify(agentProfileManageService).importProfile(any());
    }
}
