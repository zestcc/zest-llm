package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentProfileProbeCheckDiffVO {
    private String name;
    private String category;
    private boolean critical;
    private Boolean fromUp;
    private Boolean toUp;
    private String fromMessage;
    private String toMessage;
    private String changeType;
}
