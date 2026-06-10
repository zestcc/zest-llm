package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_agent_profile_probe")
public class LlmAgentProfileProbeDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private String taskCode;
    private String profileVersion;
    private String profileStatus;
    private String overallStatus;
    private Boolean ready;
    private Boolean smokeTest;
    private String probeSource;
    private Long latencyMs;
    private String checksJson;
    private LocalDateTime createdAt;
}
