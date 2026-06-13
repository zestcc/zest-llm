package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGatewayModelRequest {
    @NotBlank
    private String modelName;
    @NotBlank
    private String upstreamModel;
    private String apiBase;
    private String apiKeySecretRef;
    private String extraJson;
    private Integer sortOrder;
}
