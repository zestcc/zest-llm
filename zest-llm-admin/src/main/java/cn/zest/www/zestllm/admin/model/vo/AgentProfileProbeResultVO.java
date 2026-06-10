package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentProfileProbeResultVO {
    private String taskCode;
    private String profileVersion;
    private String profileStatus;
    private String overallStatus;
    private boolean ready;
    private long latencyMs;
    private List<AgentProfileProbeCheckVO> checks;

    private Long probeId;
    private String probeSource;
    private java.time.LocalDateTime probedAt;
}
