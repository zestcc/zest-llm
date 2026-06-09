package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_audit_log")
public class LlmAuditLogDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detailJson;
    private LocalDateTime createdAt;
}
