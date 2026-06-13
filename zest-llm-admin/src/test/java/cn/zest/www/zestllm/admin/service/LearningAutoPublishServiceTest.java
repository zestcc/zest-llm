package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.LearningAutoPublishProperties;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LearningAutoPublishServiceTest {

    @Mock
    private AgentProfilePublishService publishService;

    private LearningAutoPublishProperties properties;

    private LearningAutoPublishService service;

    @BeforeEach
    void setUp() {
        properties = new LearningAutoPublishProperties();
        service = new LearningAutoPublishService(properties, publishService);
    }

    @Test
    void shouldAutoPublish_whenEnabledAndPassedAndNotDryRun() {
        properties.setEnabled(true);
        LearningCycleResult result = LearningCycleResult.builder()
                .publishAllowed(true)
                .probePassed(true)
                .passRate(0.95)
                .build();

        assertThat(service.shouldAutoPublish(result, false)).isTrue();
    }

    @Test
    void shouldNotAutoPublish_whenDisabled() {
        properties.setEnabled(false);
        LearningCycleResult result = LearningCycleResult.builder().publishAllowed(true).build();

        assertThat(service.shouldAutoPublish(result, false)).isFalse();
    }

    @Test
    void shouldNotAutoPublish_whenDryRun() {
        properties.setEnabled(true);
        LearningCycleResult result = LearningCycleResult.builder().publishAllowed(true).build();

        assertThat(service.shouldAutoPublish(result, true)).isFalse();
    }

    @Test
    void shouldNotAutoPublish_whenPublishNotAllowed() {
        properties.setEnabled(true);
        LearningCycleResult result = LearningCycleResult.builder().publishAllowed(false).build();

        assertThat(service.shouldAutoPublish(result, false)).isFalse();
    }

    @Test
    void tryAutoPublish_invokesPublishWhenEligible() {
        properties.setEnabled(true);
        LearningCycleResult result = LearningCycleResult.builder().publishAllowed(true).build();

        service.tryAutoPublish("aiChat", "v2", result, false);

        verify(publishService).publish("aiChat", "v2", "learning-auto-publish");
    }

    @Test
    void tryAutoPublish_skipsPublishWhenAuditOnly() {
        properties.setEnabled(true);
        properties.setAuditOnly(true);
        LearningCycleResult result = LearningCycleResult.builder().publishAllowed(true).build();

        service.tryAutoPublish("aiChat", "v2", result, false);

        verify(publishService, never()).publish("aiChat", "v2", "learning-auto-publish");
    }
}
