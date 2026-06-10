package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.CapabilityStackVO;
import cn.zest.www.zestllm.admin.model.vo.StackTierVO;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CapabilityStackServiceTest {

    @Mock
    private AdapterHealthService adapterHealthService;
    @Mock
    private LlmAdapterProperties adapterProperties;

    private CapabilityStackService service;

    @BeforeEach
    void setUp() {
        service = new CapabilityStackService(adapterHealthService, adapterProperties);
        ReflectionTestUtils.setField(service, "stackTier", "medium");
        when(adapterHealthService.listAll()).thenReturn(List.of());
        when(adapterProperties.getModelGateway()).thenReturn("litellm");
        when(adapterProperties.getObservability()).thenReturn("langfuse");
        when(adapterProperties.getAgentRuntime()).thenReturn("native");
        when(adapterProperties.getKnowledgeRetrieval()).thenReturn("noop");
        when(adapterProperties.getLearningPipeline()).thenReturn("zest-eval");
    }

    @Test
    void overview_returnsCurrentTierAndTiers() {
        CapabilityStackVO vo = service.overview();
        assertEquals("medium", vo.getCurrentTier());
        assertNotNull(vo.getDeployCommand());
        assertFalse(vo.getTiers().isEmpty());
        assertEquals("litellm", vo.getRecommendedAdapters().get("model-gateway-active"));
    }

    @Test
    void getTier_large_hasDifyDefaults() {
        StackTierVO tier = service.getTier("large");
        assertEquals("large", tier.getId());
        assertEquals("dify", tier.getAdapterDefaults().get("agent-runtime"));
    }
}
