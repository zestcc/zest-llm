package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_secret_ref")
public class LlmSecretRefDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String secretCode;
    private String secretName;
    private String secretType;
    private String secretValue;
    private String envKey;
    private String scopeType;
    private Long scopeId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
