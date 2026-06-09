package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RollbackPromptRequest {
    @NotBlank
    private String version;
}
