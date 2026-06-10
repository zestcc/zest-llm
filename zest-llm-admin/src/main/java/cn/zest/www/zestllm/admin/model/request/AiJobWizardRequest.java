package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiJobWizardRequest {
    @NotBlank
    private String templateId;
    @NotBlank
    private String appKey;
    private String taskCode;
    private boolean publish;
    private boolean runProbe;
}
