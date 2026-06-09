package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenantVO {
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String status;
    private LocalDateTime createdAt;
}
