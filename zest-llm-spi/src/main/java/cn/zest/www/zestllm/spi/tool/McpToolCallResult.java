package cn.zest.www.zestllm.spi.tool;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class McpToolCallResult {
    private boolean success;
    private String toolName;
    private Map<String, Object> content;
    private String errorMessage;
}
