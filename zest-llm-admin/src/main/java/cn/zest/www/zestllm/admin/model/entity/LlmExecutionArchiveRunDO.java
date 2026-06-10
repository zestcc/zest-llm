package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_execution_archive_run")
public class LlmExecutionArchiveRunDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer archivedCount;
    private Integer deletedCount;
    private String triggerSource;
    private LocalDateTime createdAt;
}
