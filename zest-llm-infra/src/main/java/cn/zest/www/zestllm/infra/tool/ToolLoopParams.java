package cn.zest.www.zestllm.infra.tool;

import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import lombok.Builder;

import java.util.List;

@Builder
public record ToolLoopParams(
        String traceId,
        List<ToolDefinition> tools,
        Integer maxToolSteps,
        Integer maxTokens,
        Double temperature
) {
}
