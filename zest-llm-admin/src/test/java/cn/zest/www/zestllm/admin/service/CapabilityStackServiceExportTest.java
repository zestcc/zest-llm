package cn.zest.www.zestllm.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CapabilityStackServiceExportTest {

    @Mock
    private AdapterHealthService adapterHealthService;
    @Mock
    private cn.zest.www.zestllm.infra.config.LlmAdapterProperties adapterProperties;

    @InjectMocks
    private CapabilityStackService service;

    @Test
    void exportComposeEnv_medium_containsLangfuse() {
        when(adapterProperties.getModelGateway()).thenReturn("litellm");
        when(adapterProperties.getObservability()).thenReturn("noop");
        when(adapterProperties.getAgentRuntime()).thenReturn("native");
        when(adapterProperties.getKnowledgeRetrieval()).thenReturn("noop");
        when(adapterProperties.getLearningPipeline()).thenReturn("noop");

        Map<String, String> env = service.exportComposeEnv("medium");
        assertEquals("medium", env.get("ZEST_STACK_TIER"));
        assertEquals("langfuse", env.get("ZEST_LLM_ADAPTERS_OBSERVABILITY"));
    }
}
