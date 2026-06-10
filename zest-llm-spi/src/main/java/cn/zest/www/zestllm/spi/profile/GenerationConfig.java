package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

@Data
public class GenerationConfig {
    private Integer maxTokens;
    private Double temperature;
    private Integer timeoutMs;
    /** Function calling 最大轮次（对标 OpenAI Assistants max iterations） */
    private Integer maxToolSteps = 5;
}
