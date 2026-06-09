package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePromptVersionRequest {
    @NotBlank
    private String version;
    @NotBlank
    private String templateBody;
    private String outputSchema;
}
