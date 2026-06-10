package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_auth_binding")
public class LlmAuthBindingDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String scopeType;
    private Long scopeId;
    private String inboundMode;
    private String inboundConfigJson;
    private String outboundMode;
    private String outboundConfigJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
