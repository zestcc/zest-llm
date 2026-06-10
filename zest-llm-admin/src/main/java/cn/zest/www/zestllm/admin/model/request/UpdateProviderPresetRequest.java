package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProviderPresetRequest {
    @NotBlank
    private String presetName;
    private String providerType;
    private String authMode;
    @NotBlank
    private String configJson;
    private Integer sortOrder;
    private String status;
}
