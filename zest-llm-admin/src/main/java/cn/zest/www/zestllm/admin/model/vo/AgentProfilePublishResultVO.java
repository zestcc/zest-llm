package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentProfilePublishResultVO {
    private String taskCode;
    private String version;
    private String status;
    private LocalDateTime publishedAt;
    private String operator;
}
