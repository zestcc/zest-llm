package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportAgentProfileRequest {
    @NotBlank
    private String profileJson;
    private String taskCode;
    private String version;
    private boolean publish;
}
