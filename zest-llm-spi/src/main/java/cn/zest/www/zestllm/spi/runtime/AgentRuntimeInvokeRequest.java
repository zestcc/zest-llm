package cn.zest.www.zestllm.spi.runtime;

import cn.zest.www.zestllm.spi.profile.RuntimeBackendConfig;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AgentRuntimeInvokeRequest {

    private String traceId;
    private String taskCode;
    private String profileVersion;
    private RuntimeBackendConfig runtimeBackend;
    private String userMessage;
    private Map<String, Object> variables;
    private Map<String, String> headers;
}
