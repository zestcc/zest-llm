package cn.zest.www.zestllm.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://127.0.0.1:*", "http://localhost:*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/dashboard").setViewName("forward:/index.html");
        registry.addViewController("/apps").setViewName("forward:/index.html");
        registry.addViewController("/tenants").setViewName("forward:/index.html");
        registry.addViewController("/tasks").setViewName("forward:/index.html");
        registry.addViewController("/prompts").setViewName("forward:/index.html");
        registry.addViewController("/model-routes").setViewName("forward:/index.html");
        registry.addViewController("/executions").setViewName("forward:/index.html");
        registry.addViewController("/registry").setViewName("forward:/index.html");
        registry.addViewController("/audit-logs").setViewName("forward:/index.html");
        registry.addViewController("/adapters").setViewName("forward:/index.html");
        registry.addViewController("/agent-config").setViewName("forward:/index.html");
        registry.addViewController("/ops").setViewName("forward:/index.html");
        registry.addViewController("/playground").setViewName("forward:/index.html");
        registry.addViewController("/eval").setViewName("forward:/index.html");
        registry.addViewController("/flow-chains").setViewName("forward:/index.html");
        registry.addViewController("/users").setViewName("forward:/index.html");
    }
}
