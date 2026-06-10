package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RollbackAgentProfileRequest {
    @NotBlank
    private String version;
}
