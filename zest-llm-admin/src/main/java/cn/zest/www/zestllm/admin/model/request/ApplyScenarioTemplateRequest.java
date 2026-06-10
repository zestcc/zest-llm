package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplyScenarioTemplateRequest {
    @NotBlank
    private String templateId;
    @NotBlank
    private String appKey;
    private String taskCode;
    private boolean publish;
}
