package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class UpdateGatewayModelRequest {
    private String upstreamModel;
    private String apiBase;
    private String apiKeySecretRef;
    private String extraJson;
    private Integer sortOrder;
    private String status;
}
