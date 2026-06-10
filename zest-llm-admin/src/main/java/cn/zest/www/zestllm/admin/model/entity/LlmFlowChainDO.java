package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_flow_chain")
public class LlmFlowChainDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String chainCode;
    private String chainName;
    private Integer version;
    private String lifecycle;
    private String chainData;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
