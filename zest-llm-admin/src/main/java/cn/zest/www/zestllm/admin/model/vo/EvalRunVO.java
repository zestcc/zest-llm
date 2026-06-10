package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EvalRunVO {
    private String runCode;
    private String datasetCode;
    private String status;
    private Integer totalCases;
    private Integer passedCases;
    private Integer failedCases;
    private BigDecimal passRate;
    private List<Map<String, Object>> caseResults;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
