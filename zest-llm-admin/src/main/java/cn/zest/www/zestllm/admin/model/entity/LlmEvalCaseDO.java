package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_eval_case")
public class LlmEvalCaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long datasetId;
    private String caseCode;
    private String inputsJson;
    private String expectedJson;
    private String status;
    private LocalDateTime createdAt;
}
