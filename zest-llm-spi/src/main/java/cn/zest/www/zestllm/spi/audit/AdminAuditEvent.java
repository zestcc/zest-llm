package cn.zest.www.zestllm.spi.audit;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminAuditEvent {
    private String operator;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private Instant timestamp;
}
