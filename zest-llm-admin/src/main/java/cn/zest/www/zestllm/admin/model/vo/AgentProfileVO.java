package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentProfileVO {
    private Long id;
    private String taskCode;
    private String version;
    private String profileJson;
    private String providerPresetCode;
    private String runtimeMode;
    private String status;
    private LocalDateTime publishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
