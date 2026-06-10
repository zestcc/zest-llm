package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptDiffServiceTest {

    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmPromptVersionRepo promptVersionRepo;
    @InjectMocks
    private PromptDiffService promptDiffService;

    @Test
    void diff_detectsTemplateChange() {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(1L);
        task.setCode("aiChat");
        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));

        LlmPromptVersionDO v1 = new LlmPromptVersionDO();
        v1.setVersion("v1");
        v1.setTemplateBody("hello");
        v1.setStatus("PUBLISHED");
        LlmPromptVersionDO v2 = new LlmPromptVersionDO();
        v2.setVersion("v2");
        v2.setTemplateBody("hello world");
        v2.setStatus("DRAFT");
        when(promptVersionRepo.findByTaskIdAndVersion(1L, "v1")).thenReturn(Optional.of(v1));
        when(promptVersionRepo.findByTaskIdAndVersion(1L, "v2")).thenReturn(Optional.of(v2));

        var diff = promptDiffService.diff("aiChat", "v1", "v2");
        assertFalse(diff.getChanges().isEmpty());
    }
}
