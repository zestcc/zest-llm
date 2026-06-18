package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.request.ForkPromptVersionRequest;
import cn.zest.www.zestllm.admin.model.vo.PromptForkResultVO;
import cn.zest.www.zestllm.admin.model.vo.PromptPublishResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptVersionServiceTest {

    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmPromptVersionRepo promptVersionRepo;
    @Mock
    private PromptPublishService promptPublishService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PromptVersionService service;

    @Test
    void suggestNextVersion_incrementsMaxNumericVersion() {
        LlmAiTaskDefDO task = task(1L, "aiChat");
        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));
        when(promptVersionRepo.findByTaskId(1L)).thenReturn(List.of(
                prompt("v1"), prompt("v2")));

        assertEquals("v3", service.suggestNextVersion("aiChat"));
    }

    @Test
    void forkVersion_createsAndPublishes() {
        LlmAiTaskDefDO task = task(1L, "aiChat");
        LlmPromptVersionDO base = prompt("v2");
        base.setTemplateBody("old");
        base.setOutputSchema("{\"type\":\"object\"}");

        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));
        when(promptVersionRepo.findByTaskIdAndVersion(1L, "v2")).thenReturn(Optional.of(base));
        when(promptVersionRepo.findByTaskIdAndVersion(1L, "v3")).thenReturn(Optional.empty(), Optional.of(base));
        when(promptPublishService.publish(eq("aiChat"), eq("v3"), eq(null)))
                .thenReturn(PromptPublishResultVO.builder().version("v3").build());

        ForkPromptVersionRequest request = new ForkPromptVersionRequest();
        request.setBaseVersion("v2");
        request.setVersion("v3");
        request.setTemplateBody("new body");
        request.setPublish(true);

        PromptForkResultVO result = service.forkVersion("aiChat", request);

        assertEquals("v3", result.getVersion());
        assertTrue(result.isPublished());
        verify(promptVersionRepo).insert(org.mockito.ArgumentMatchers.any());
        verify(promptPublishService).publish("aiChat", "v3", null);
    }

    @Test
    void forkVersion_rejectsDuplicateVersion() {
        LlmAiTaskDefDO task = task(1L, "aiChat");
        LlmPromptVersionDO base = prompt("v2");
        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));
        when(promptVersionRepo.findByTaskIdAndVersion(1L, "v2")).thenReturn(Optional.of(base));
        when(promptVersionRepo.findByTaskIdAndVersion(1L, "v3")).thenReturn(Optional.of(prompt("v3")));

        ForkPromptVersionRequest request = new ForkPromptVersionRequest();
        request.setBaseVersion("v2");
        request.setVersion("v3");
        request.setTemplateBody("body");
        request.setPublish(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.forkVersion("aiChat", request));
        assertEquals("VERSION_EXISTS", ex.getErrorCode());
    }

    private static LlmAiTaskDefDO task(Long id, String code) {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(id);
        task.setCode(code);
        return task;
    }

    private static LlmPromptVersionDO prompt(String version) {
        LlmPromptVersionDO prompt = new LlmPromptVersionDO();
        prompt.setVersion(version);
        return prompt;
    }
}
