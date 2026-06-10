package cn.zest.www.zestllm.spi.profile;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileExtensionsTest {

    @Test
    void parsesIntegrationExtensions() {
        AgentProfileDocument profile = new AgentProfileDocument();
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("runtimeBackend", Map.of(
                "type", "dify",
                "baseUrl", "http://dify:5001",
                "externalAppId", "app-1"
        ));
        extensions.put("knowledge", Map.of(
                "enabled", true,
                "provider", "ragflow",
                "datasetIds", List.of("kb-1"),
                "topK", 3
        ));
        extensions.put("learningLoop", Map.of(
                "enabled", true,
                "evalDatasetRef", "regression@v1",
                "minPassRate", 0.9
        ));
        profile.setExtensions(extensions);

        RuntimeBackendConfig backend = ProfileExtensions.runtimeBackend(profile).orElseThrow();
        assertEquals("dify", backend.getType());
        assertEquals("app-1", backend.getExternalAppId());

        KnowledgeRefConfig knowledge = ProfileExtensions.knowledge(profile).orElseThrow();
        assertTrue(knowledge.isEnabled());
        assertEquals(3, knowledge.getTopK());

        LearningLoopConfig loop = ProfileExtensions.learningLoop(profile).orElseThrow();
        assertEquals("regression@v1", loop.getEvalDatasetRef());
        assertEquals(0.9, loop.getMinPassRate());
    }
}
