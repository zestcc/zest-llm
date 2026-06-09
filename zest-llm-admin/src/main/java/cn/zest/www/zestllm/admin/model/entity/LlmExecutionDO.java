package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_execution")
public class LlmExecutionDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String traceId;
    private Long appId;
    private Long taskId;
    private String taskCode;
    private String bizId;
    private String promptVersion;
    private String model;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorCode;
    private String errorMessage;
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private BigDecimal cost;
    private String flowExecutionId;
    private LocalDateTime createdAt;
}
