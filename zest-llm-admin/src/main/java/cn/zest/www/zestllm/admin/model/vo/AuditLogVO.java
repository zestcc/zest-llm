package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogVO {
    private Long id;
    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detailJson;
    private LocalDateTime createdAt;
}
