package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class UpdateSecretRefRequest {
    private String secretName;
    private String secretType;
    private String secretValue;
    private String envKey;
    private String scopeType;
    private Long scopeId;
    private String status;
}
