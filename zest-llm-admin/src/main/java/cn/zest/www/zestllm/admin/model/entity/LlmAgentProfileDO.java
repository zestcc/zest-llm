package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_agent_profile")
public class LlmAgentProfileDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private String version;
    private String profileJson;
    private String providerPresetCode;
    private String runtimeMode;
    private String status;
    private LocalDateTime publishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
