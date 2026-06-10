package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublishAgentProfileRequest {
    @NotBlank
    private String version;
    private String operator;
}
