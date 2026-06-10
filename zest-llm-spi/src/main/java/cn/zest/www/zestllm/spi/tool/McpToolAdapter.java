package cn.zest.www.zestllm.spi.tool;

/**
 * Executes MCP tool calls (JSON-RPC tools/call) for agent runtime.
 */
public interface McpToolAdapter {

    McpToolCallResult call(McpToolCallRequest request);

    default java.util.List<McpToolDescriptor> listTools(McpToolListRequest request) {
        return java.util.Collections.emptyList();
    }
}
