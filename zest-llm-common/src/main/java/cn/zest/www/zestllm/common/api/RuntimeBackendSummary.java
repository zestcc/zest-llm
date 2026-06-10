package cn.zest.www.zestllm.common.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuntimeBackendSummary {
    private String type;
    private String baseUrl;
    private String externalAppId;
    private String protocol;
    private Integer timeoutMs;
}
