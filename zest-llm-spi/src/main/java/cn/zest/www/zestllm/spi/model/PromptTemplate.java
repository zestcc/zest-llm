package cn.zest.www.zestllm.spi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PromptTemplate {
    private String templateBody;
    private String version;
}
