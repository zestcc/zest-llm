package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_learning_cycle_run")
public class LlmLearningCycleRunDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskCode;
    private String profileVersion;
    private String runCode;
    private BigDecimal passRate;
    private Boolean probePassed;
    private Boolean publishAllowed;
    private String status;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
