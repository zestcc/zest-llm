package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GatewayModelVO {
    private Long id;
    private String modelName;
    private String upstreamModel;
    private String apiBase;
    private String apiKeySecretRef;
    private String extraJson;
    private String status;
    private String syncStatus;
    private LocalDateTime lastSyncAt;
    private Integer sortOrder;
    private LocalDateTime updatedAt;
}
