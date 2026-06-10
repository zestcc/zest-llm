package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class UpdateMcpServerRequest {
    private String serverName;
    private String baseUrl;
    private String authSecretRef;
    private String configJson;
    private String status;
}
