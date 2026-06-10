package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentProfileProbeCompareVO {
    private String taskCode;
    private String fromVersion;
    private String toVersion;
    private String fromStatus;
    private String toStatus;
    private List<AgentProfileProbeCheckDiffVO> diffs;
}
