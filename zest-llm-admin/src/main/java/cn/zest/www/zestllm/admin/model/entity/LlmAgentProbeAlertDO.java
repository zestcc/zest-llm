package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_agent_probe_alert")
public class LlmAgentProbeAlertDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private String taskCode;
    private String profileVersion;
    private String overallStatus;
    private Long probeId;
    private String webhookUrl;
    private String status;
    private String detailJson;
    private LocalDateTime createdAt;
}
