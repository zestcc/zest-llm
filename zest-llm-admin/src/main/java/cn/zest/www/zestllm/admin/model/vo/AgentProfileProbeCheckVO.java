package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentProfileProbeCheckVO {
    private String name;
    private String category;
    private boolean critical;
    private boolean up;
    private String message;
}
