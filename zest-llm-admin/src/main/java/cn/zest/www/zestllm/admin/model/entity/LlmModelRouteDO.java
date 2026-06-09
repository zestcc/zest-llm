package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_model_route")
public class LlmModelRouteDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private String primaryModel;
    private String fallbackModels;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Integer timeoutMs;
    private String policyJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
