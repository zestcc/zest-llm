package cn.zest.www.zestllm.spi.tool;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class McpToolCallRequest {
    private String serverUrl;
    private String serverAuthToken;
    private String toolName;
    private Map<String, Object> arguments;
    private String traceId;
}
