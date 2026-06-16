package cn.zest.www.zestllm.plugin.prompt.handlebars;

import cn.zest.www.zestllm.spi.model.PromptTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandlebarsPromptRendererTest {

  private final HandlebarsPromptRenderer renderer = new HandlebarsPromptRenderer();

  @Test
  void shouldRenderTaskFlagBlocks() {
    PromptTemplate template = PromptTemplate.builder()
        .templateBody("""
            {{#if taskContinue}}
            CONTINUE_BLOCK
            {{/if}}
            {{#if taskPolish}}
            POLISH_BLOCK
            {{/if}}
            """)
        .build();

    String continueRendered = renderer.render(template, Map.of("taskContinue", true, "taskPolish", false));
    assertTrue(continueRendered.contains("CONTINUE_BLOCK"));
    assertFalse(continueRendered.contains("POLISH_BLOCK"));
  }

  @Test
  void shouldRenderSystemPromptSlot() {
    PromptTemplate template = PromptTemplate.builder()
        .templateBody("""
            {{#if systemPrompt}}
            FLAVOR:{{systemPrompt}}
            {{/if}}
            CTX:{{userMessage}}
            """)
        .build();

    String rendered = renderer.render(template, Map.of(
        "systemPrompt", "废土冷峻",
        "userMessage", "【书籍】测试"));

    assertTrue(rendered.contains("FLAVOR:废土冷峻"));
    assertTrue(rendered.contains("CTX:【书籍】测试"));
  }
}
