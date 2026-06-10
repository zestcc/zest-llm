package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.AdminObservabilityProperties;
import cn.zest.www.zestllm.infra.config.LangfuseProperties;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObservabilityLinkServiceTest {

    private ObservabilityLinkService linkService;

    @BeforeEach
    void setUp() {
        LlmAdapterProperties adapters = new LlmAdapterProperties();
        adapters.setObservability("langfuse");
        LangfuseProperties langfuse = new LangfuseProperties();
        langfuse.setEnabled(true);
        langfuse.setBaseUrl("http://langfuse:3000");
        AdminObservabilityProperties adminObs = new AdminObservabilityProperties();
        adminObs.setLangfuseUiBaseUrl("http://localhost:3000");
        adminObs.setTracePathTemplate("/trace/{traceId}");
        linkService = new ObservabilityLinkService(adapters, langfuse, adminObs);
    }

    @Test
    void buildTraceUrl_whenLangfuseEnabled() {
        assertEquals("http://localhost:3000/trace/tr_abc", linkService.buildTraceUrl("tr_abc"));
    }

    @Test
    void buildTraceUrl_whenNoopAdapter() {
        LlmAdapterProperties adapters = new LlmAdapterProperties();
        adapters.setObservability("noop");
        linkService = new ObservabilityLinkService(adapters, new LangfuseProperties(), new AdminObservabilityProperties());
        assertNull(linkService.buildTraceUrl("tr_abc"));
    }
}
