package cn.zest.www.zestllm.spi.tool;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpToolListRequest {
    private String serverUrl;
    private String serverAuthToken;
}
