package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.request.ApplyScenarioTemplateRequest;
import cn.zest.www.zestllm.admin.model.request.ImportAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileVO;
import cn.zest.www.zestllm.admin.model.vo.ApplyScenarioTemplateResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioTemplateServiceTest {

    @Mock
    private AgentProfileManageService agentProfileManageService;
    @Mock
    private AgentProfilePublishService agentProfilePublishService;
    @Mock
    private AgentProfileResolver agentProfileResolver;
    @Mock
    private TaskManageService taskManageService;
    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmAgentProfileRepo agentProfileRepo;

    private ScenarioTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ScenarioTemplateService(
                agentProfileManageService,
                agentProfilePublishService,
                agentProfileResolver,
                taskManageService,
                taskDefRepo,
                agentProfileRepo,
                new ObjectMapper());
        service.loadTemplates();
    }

    @Test
    void listTemplates_loadsBundledTemplates() {
        List<?> templates = service.listTemplates();
        assertTrue(templates.size() >= 4);
    }

    @Test
    void getTemplate_knowledgeQa_hasKnowledgeAndLearning() {
        var tpl = service.getTemplate("knowledge-qa");
        assertEquals("knowledge-qa", tpl.getId());
        assertEquals("知识问答", tpl.getName());
        assertTrue(tpl.isRequiresKnowledge());
        assertEquals("medium", tpl.getRecommendedTier());
    }

    @Test
    void apply_unknownTemplate_throws() {
        ApplyScenarioTemplateRequest req = new ApplyScenarioTemplateRequest();
        req.setTemplateId("missing");
        req.setAppKey("order-service");
        assertThrows(BusinessException.class, () -> service.apply(req));
    }

    @Test
    void apply_chatBasic_createsStableDraftVersion() {
        LlmAiTaskDefDO task = taskEntity();
        stubProfileDocument();
        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.empty(), Optional.of(task));
        when(agentProfileRepo.findByTaskIdAndVersion(1L, "v-tpl-chatbasic")).thenReturn(Optional.empty());
        when(agentProfileManageService.importProfile(any())).thenReturn(
                AgentProfileVO.builder().version("v-tpl-chatbasic").build());

        ApplyScenarioTemplateRequest req = new ApplyScenarioTemplateRequest();
        req.setTemplateId("chat-basic");
        req.setAppKey("order-service");
        req.setTaskCode("aiChat");

        ApplyScenarioTemplateResultVO result = service.apply(req);
        assertEquals("aiChat", result.getTaskCode());
        ArgumentCaptor<ImportAgentProfileRequest> captor = ArgumentCaptor.forClass(ImportAgentProfileRequest.class);
        verify(taskManageService).create(any());
        verify(agentProfileManageService).importProfile(captor.capture());
        assertEquals("v-tpl-chatbasic", captor.getValue().getVersion());
    }

    @Test
    void apply_chatBasic_updatesExistingDraft() {
        LlmAiTaskDefDO task = taskEntity();
        LlmAgentProfileDO draft = new LlmAgentProfileDO();
        draft.setStatus("DRAFT");
        stubProfileDocument();
        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));
        when(agentProfileRepo.findByTaskIdAndVersion(1L, "v-tpl-chatbasic")).thenReturn(Optional.of(draft));
        when(agentProfileManageService.updateVersion(eq("aiChat"), eq("v-tpl-chatbasic"), any(UpdateAgentProfileRequest.class)))
                .thenReturn(AgentProfileVO.builder().version("v-tpl-chatbasic").build());

        ApplyScenarioTemplateRequest req = new ApplyScenarioTemplateRequest();
        req.setTemplateId("chat-basic");
        req.setAppKey("order-service");
        req.setTaskCode("aiChat");

        service.apply(req);
        verify(agentProfileManageService).updateVersion(eq("aiChat"), eq("v-tpl-chatbasic"), any(UpdateAgentProfileRequest.class));
        verify(agentProfileManageService, never()).importProfile(any());
    }

    private LlmAiTaskDefDO taskEntity() {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(1L);
        task.setCode("aiChat");
        return task;
    }

    private void stubProfileDocument() {
        AgentProfileDocument doc = new AgentProfileDocument();
        doc.setProviderRef("litellm-default");
        doc.setRuntimeMode("agent");
        when(agentProfileResolver.parseProfile(any(), isNull())).thenReturn(doc);
    }
}
