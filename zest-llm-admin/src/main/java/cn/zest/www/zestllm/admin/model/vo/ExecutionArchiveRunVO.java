package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionArchiveRunVO {
    private Long id;
    private Integer archivedCount;
    private Integer deletedCount;
    private String triggerSource;
    private LocalDateTime createdAt;
}
