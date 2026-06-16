package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileProbeRepo;
import cn.zest.www.zestllm.admin.repo.LlmAgentProbeAlertRepo;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskManageServiceTest {

    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmAppRepo appRepo;
    @Mock
    private LlmPromptVersionRepo promptVersionRepo;
    @Mock
    private LlmModelRouteRepo modelRouteRepo;
    @Mock
    private LlmAgentProfileRepo agentProfileRepo;
    @Mock
    private LlmAgentProfileProbeRepo agentProfileProbeRepo;
    @Mock
    private LlmAgentProbeAlertRepo agentProbeAlertRepo;
    @Mock
    private LlmExecutionRepo executionRepo;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private TaskManageService service;

    @Test
    void delete_rejectsWhenPromptVersionsExist() {
        LlmAiTaskDefDO task = task(1L, "demoTask");
        when(taskDefRepo.findByCode("demoTask")).thenReturn(Optional.of(task));
        when(promptVersionRepo.findByTaskId(1L)).thenReturn(List.of(new LlmPromptVersionDO()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("demoTask"));
        assertEquals("TASK_NOT_DELETABLE", ex.getErrorCode());
        verify(taskDefRepo, never()).deleteById(1L);
    }

    @Test
    void delete_succeedsWhenNoDependencies() {
        LlmAiTaskDefDO task = task(2L, "emptyTask");
        when(taskDefRepo.findByCode("emptyTask")).thenReturn(Optional.of(task));
        when(promptVersionRepo.findByTaskId(2L)).thenReturn(List.of());
        when(modelRouteRepo.countByTaskId(2L)).thenReturn(0L);
        when(agentProfileRepo.findByTaskId(2L)).thenReturn(List.of());
        when(executionRepo.countByTaskCode("emptyTask")).thenReturn(0L);
        when(executionRepo.countArchivedByTaskCode("emptyTask")).thenReturn(0L);

        service.delete("emptyTask");

        verify(taskDefRepo).deleteById(2L);
    }

    @Test
    void forceDeleteById_cascadesArtifacts() {
        LlmAiTaskDefDO task = task(9L, "zestStoryReview");
        when(taskDefRepo.findById(9L)).thenReturn(Optional.of(task));

        service.forceDeleteById(9L);

        verify(agentProfileProbeRepo).deleteByTaskId(9L);
        verify(agentProbeAlertRepo).deleteByTaskId(9L);
        verify(agentProfileRepo).deleteByTaskId(9L);
        verify(promptVersionRepo).deleteByTaskId(9L);
        verify(modelRouteRepo).deleteByTaskId(9L);
        verify(taskDefRepo).deleteById(9L);
    }

    private static LlmAiTaskDefDO task(Long id, String code) {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(id);
        task.setCode(code);
        task.setName(code);
        return task;
    }
}
