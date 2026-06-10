package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentProbeAlertVO {
    private Long id;
    private String taskCode;
    private String profileVersion;
    private String overallStatus;
    private Long probeId;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
