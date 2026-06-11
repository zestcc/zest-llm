package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProviderDefinition {
    private String type = "litellm";
    private String baseUrl;
    /** openai（默认）或 anthropic（LiteLLM /v1/messages） */
    private String protocol = "openai";
    private String authMode = "API_KEY";
    private Map<String, Object> headers = new LinkedHashMap<>();
    private Map<String, Object> extra = new LinkedHashMap<>();
}
