package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskRequest {
    @NotBlank
    private String appKey;
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String description;
}
