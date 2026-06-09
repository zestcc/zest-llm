package cn.zest.www.zestllm.spi.cache;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CachedPolicy {
    private String promptVersion;
    private String templateBody;
    private String outputSchema;
    private String primaryModel;
    private List<String> fallbackModels;
    private Integer maxTokens;
    private Double temperature;
    private Integer timeoutMs;
}
