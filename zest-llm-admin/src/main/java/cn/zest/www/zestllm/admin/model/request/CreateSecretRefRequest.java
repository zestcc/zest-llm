package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSecretRefRequest {
    @NotBlank
    private String secretCode;
    @NotBlank
    private String secretName;
    @NotBlank
    private String secretType;
    private String secretValue;
    private String envKey;
    private String scopeType;
    private Long scopeId;
}
