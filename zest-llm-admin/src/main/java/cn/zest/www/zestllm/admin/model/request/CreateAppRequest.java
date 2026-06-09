package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAppRequest {
    @NotBlank
    private String appKey;
    @NotBlank
    private String appName;
    private String tenantCode;
}
