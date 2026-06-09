package cn.zest.www.zestllm.admin.model.dto;

import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolvedPolicy {
    private LlmAppDO app;
    private LlmAiTaskDefDO task;
    private CachedPolicy policy;
    private String renderedPrompt;
}
