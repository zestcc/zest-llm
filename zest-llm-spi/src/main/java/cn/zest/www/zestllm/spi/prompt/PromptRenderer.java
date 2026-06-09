package cn.zest.www.zestllm.spi.prompt;

import cn.zest.www.zestllm.spi.model.PromptTemplate;

import java.util.Map;

/**
 * Prompt 模板渲染 SPI。
 */
public interface PromptRenderer {

    String rendererId();

    String render(PromptTemplate template, Map<String, Object> variables);
}
