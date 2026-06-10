package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.dto.PublishPromptCommand;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.ResponseCacheAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptPublishServiceTest {

    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmPromptVersionRepo promptVersionRepo;
    @Mock
    private LlmAppRepo appRepo;
    @Mock
    private PolicyCacheAdapter policyCacheAdapter;
    @Mock
    private ResponseCacheAdapter responseCacheAdapter;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PromptPublishService promptPublishService;

    @Test
    void publish_invalidatesPolicyCache() {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(10L);
        task.setAppId(20L);
        task.setCode("aiChat");

        LlmAppDO app = new LlmAppDO();
        app.setId(20L);
        app.setAppKey("order-service");

        LlmPromptVersionDO prompt = new LlmPromptVersionDO();
        prompt.setVersion("v2");

        when(taskDefRepo.findByCode("aiChat")).thenReturn(Optional.of(task));
        when(promptVersionRepo.findByTaskIdAndVersion(10L, "v2")).thenReturn(Optional.of(prompt));
        when(appRepo.findById(20L)).thenReturn(Optional.of(app));

        PublishPromptCommand command = new PublishPromptCommand();
        command.setTaskCode("aiChat");
        command.setVersion("v2");
        command.setOperator("admin");

        promptPublishService.publish(command);

        verify(policyCacheAdapter).invalidate(eq("order-service"), eq("aiChat"));
        verify(responseCacheAdapter).invalidate(eq("order-service"), eq("aiChat"));
    }
}
