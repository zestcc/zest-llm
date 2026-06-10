package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProviderPresetRequest {
    @NotBlank
    private String presetCode;
    @NotBlank
    private String presetName;
    private String providerType;
    private String authMode;
    @NotBlank
    private String configJson;
    private Integer sortOrder;
    private String tenantCode;
}
