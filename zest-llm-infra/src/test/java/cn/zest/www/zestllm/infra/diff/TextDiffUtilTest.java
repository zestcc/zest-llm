package cn.zest.www.zestllm.infra.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextDiffUtilTest {

    @Test
    void unifiedLineDiff_showsAddedAndRemovedLines() {
        String diff = TextDiffUtil.unifiedLineDiff("line1\nline2", "line1\nline3");
        assertTrue(diff.contains("-line2"));
        assertTrue(diff.contains("+line3"));
        assertTrue(diff.contains(" line1"));
    }
}
