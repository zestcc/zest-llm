package cn.zest.www.zestllm.spi.tool;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpToolDescriptor {
    private String name;
    private String description;
    private Object inputSchema;
}
