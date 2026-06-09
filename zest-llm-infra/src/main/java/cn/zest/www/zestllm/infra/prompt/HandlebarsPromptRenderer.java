package cn.zest.www.zestllm.infra.prompt;

import cn.zest.www.zestllm.spi.model.PromptTemplate;
import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class HandlebarsPromptRenderer implements PromptRenderer {

    private final Handlebars handlebars = new Handlebars();

    @Override
    public String rendererId() {
        return "handlebars";
    }

    @Override
    public String render(PromptTemplate template, Map<String, Object> variables) {
        try {
            Template compiled = handlebars.compileInline(template.getTemplateBody());
            return compiled.apply(variables == null ? Map.of() : variables);
        } catch (IOException ex) {
            throw new IllegalStateException("Prompt render failed: " + ex.getMessage(), ex);
        }
    }
}
