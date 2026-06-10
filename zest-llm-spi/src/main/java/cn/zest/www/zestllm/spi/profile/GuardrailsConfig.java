package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

@Data
public class GuardrailsConfig {
    private Boolean piiRedact;
    private Boolean blockOnSchemaMismatch = true;
    private Integer maxPromptLength;
    /** 启用关键词黑名单（ContentModeration SPI） */
    private Boolean blockPromptKeywords;
    /** 自定义黑名单；为空时使用平台默认越狱短语 */
    private java.util.List<String> blockedKeywords;
}
