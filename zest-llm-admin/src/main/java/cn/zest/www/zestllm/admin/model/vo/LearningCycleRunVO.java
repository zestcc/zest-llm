package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LearningCycleRunVO {
    private String runCode;
    private String taskCode;
    private String profileVersion;
    private BigDecimal passRate;
    private Boolean probePassed;
    private Boolean publishAllowed;
    private String status;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
