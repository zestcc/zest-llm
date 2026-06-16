package cn.zest.www.zestllm.plugin.prompt.handlebars;

import cn.zest.www.zestllm.spi.model.PromptTemplate;
import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class HandlebarsPromptRenderer implements PromptRenderer {

    private final Handlebars handlebars;

    public HandlebarsPromptRenderer() {
        this.handlebars = new Handlebars();
        registerHelpers(this.handlebars);
    }

    static void registerHelpers(Handlebars handlebars) {
        handlebars.registerHelper("eq", (context, options) -> {
            if (options.params.length < 2) {
                return options.inverse();
            }
            Object left = options.param(0);
            Object right = options.param(1);
            boolean match = Objects.equals(
                    left == null ? null : String.valueOf(left),
                    right == null ? null : String.valueOf(right));
            return match ? options.fn() : options.inverse();
        });
    }

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
