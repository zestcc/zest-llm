package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentHealthItemVO {
    private String taskCode;
    private String profileVersion;
    private String overallStatus;
    private boolean ready;
    private Long latencyMs;
    private LocalDateTime probedAt;
    private String probeSource;
}
