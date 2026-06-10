package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpsertAuthBindingRequest {
    @NotBlank
    private String scopeType;
    private Long scopeId;
    private String appKey;
    private String taskCode;
    @NotBlank
    private String inboundMode;
    private String inboundConfigJson;
    private String outboundMode;
    private String outboundConfigJson;
}
