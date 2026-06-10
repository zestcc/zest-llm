package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_eval_dataset")
public class LlmEvalDatasetDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String datasetCode;
    private String datasetName;
    private String appKey;
    private String taskCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
