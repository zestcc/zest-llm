package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileExtensionsValidatorTest {

    private final ProfileExtensionsValidator validator = new ProfileExtensionsValidator();

    @Test
    void acceptsHybridWithKnowledge() {
        AgentProfileDocument doc = baseDoc("hybrid");
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("knowledge", Map.of(
                "enabled", true,
                "datasetIds", List.of("kb-1"),
                "injectMode", "system_prefix"
        ));
        doc.setExtensions(ext);
        assertDoesNotThrow(() -> validator.validate(doc));
    }

    @Test
    void rejectsExternalWithoutBackend() {
        AgentProfileDocument doc = new AgentProfileDocument();
        doc.setRuntimeMode("external");
        doc.setExtensions(new LinkedHashMap<>());
        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validate(doc));
        assertEquals("INVALID_PROFILE", ex.getErrorCode());
    }

    private AgentProfileDocument baseDoc(String runtimeMode) {
        AgentProfileDocument doc = new AgentProfileDocument();
        doc.setRuntimeMode(runtimeMode);
        doc.setExtensions(new LinkedHashMap<>());
        return doc;
    }
}
