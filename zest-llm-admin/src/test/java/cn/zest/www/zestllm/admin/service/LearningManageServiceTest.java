package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmLearningCycleRunRepo;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningManageServiceTest {

    @Mock
    private ZestEvalLearningPipelineAdapter learningPipelineAdapter;
    @Mock
    private AgentProfileResolver agentProfileResolver;
    @Mock
    private LlmLearningCycleRunRepo learningCycleRunRepo;
    @Mock
    private LlmAiTaskDefRepo taskDefRepo;
    @Mock
    private LlmAgentProfileRepo agentProfileRepo;
    @Mock
    private LearningAutoPublishService learningAutoPublishService;

    @InjectMocks
    private LearningManageService learningManageService;

    @Test
    void runScheduledCycles_skipsProfilesWithoutLearningLoop() {
        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setTaskId(1L);
        profile.setVersion("v1");
        profile.setProfileJson("{}");
        when(agentProfileRepo.findAllPublished()).thenReturn(List.of(profile));
        when(taskDefRepo.findById(1L)).thenReturn(Optional.of(task("aiChat")));
        when(agentProfileResolver.parseProfile("{}", null)).thenReturn(new AgentProfileDocument());

        int count = learningManageService.runScheduledCycles(true);

        assertThat(count).isZero();
        verify(learningPipelineAdapter, never()).runCycle(any());
    }

    @Test
    void runScheduledCycles_runsWhenLearningLoopEnabled() {
        LlmAgentProfileDO profile = new LlmAgentProfileDO();
        profile.setTaskId(1L);
        profile.setVersion("v1");
        profile.setProfileJson("{\"extensions\":{\"learningLoop\":{\"enabled\":true}}}");
        AgentProfileDocument document = new AgentProfileDocument();
        LearningLoopConfig loop = new LearningLoopConfig();
        loop.setEnabled(true);
        document.setExtensions(java.util.Map.of("learningLoop", loop));

        when(agentProfileRepo.findAllPublished()).thenReturn(List.of(profile));
        when(taskDefRepo.findById(1L)).thenReturn(Optional.of(task("reportGen")));
        when(taskDefRepo.findByCode("reportGen")).thenReturn(Optional.of(task("reportGen")));
        when(agentProfileRepo.findByTaskIdAndVersion(1L, "v1")).thenReturn(Optional.of(profile));
        when(agentProfileResolver.parseProfile(profile.getProfileJson(), null)).thenReturn(document);
        when(learningPipelineAdapter.runCycle(any())).thenReturn(LearningCycleResult.builder().passRate(1.0).build());

        int count = learningManageService.runScheduledCycles(true);

        assertThat(count).isEqualTo(1);
        verify(learningPipelineAdapter).runCycle(any());
    }

    private static LlmAiTaskDefDO task(String code) {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(1L);
        task.setCode(code);
        return task;
    }
}
