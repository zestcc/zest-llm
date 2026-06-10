package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMcpServerRequest {
    @NotBlank
    private String serverCode;
    @NotBlank
    private String serverName;
    @NotBlank
    private String baseUrl;
    private String authSecretRef;
    private String configJson;
}
