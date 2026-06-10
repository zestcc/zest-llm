package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_eval_run")
public class LlmEvalRunDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long datasetId;
    private String runCode;
    private String status;
    private Integer totalCases;
    private Integer passedCases;
    private Integer failedCases;
    private BigDecimal passRate;
    private String reportJson;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
