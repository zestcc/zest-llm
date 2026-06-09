package cn.zest.www.zestllm.common.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class PrepareRequest {
    @NotBlank
    private String appKey;
    @NotBlank
    private String code;
    private String bizId;
    private Map<String, Object> inputs;
    private Map<String, Object> context;
    private InvokeOptions options;
}
