package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAgentProfileRequest {
    @NotBlank
    private String version;
    @NotBlank
    private String profileJson;
    private String providerPresetCode;
    private String runtimeMode;
}
