package cn.zest.www.zestllm.admin.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptVariableEnricherTest {

    @Test
    void shouldSetTaskFlagsFromTaskType() {
        Map<String, Object> enriched = PromptVariableEnricher.enrich(Map.of("taskType", "continue"));

        assertTrue((Boolean) enriched.get("taskContinue"));
        assertFalse((Boolean) enriched.get("taskPolish"));
        assertEquals("CONTINUE", enriched.get("taskType"));
    }

    @Test
    void shouldExposeSegmentProseFlag() {
        Map<String, Object> enriched = PromptVariableEnricher.enrich(Map.of("taskType", "AUTO_CHAPTER_PROSE"));

        assertTrue((Boolean) enriched.get("taskAutoChapterProse"));
        assertFalse((Boolean) enriched.get("taskAutoChapter"));
    }
}
