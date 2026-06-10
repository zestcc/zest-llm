package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAgentProfileRequest {
    @NotBlank
    private String profileJson;
    private String providerPresetCode;
    private String runtimeMode;
}
