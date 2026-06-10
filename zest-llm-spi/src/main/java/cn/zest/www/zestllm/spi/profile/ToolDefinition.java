package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ToolDefinition {
    private String type;
    private String name;
    private String serverRef;
    private Map<String, Object> config = new LinkedHashMap<>();
}
