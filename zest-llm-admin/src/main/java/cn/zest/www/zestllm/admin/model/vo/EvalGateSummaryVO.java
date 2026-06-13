package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvalGateSummaryVO {
    private boolean enabled;
    private double passRate;
    private int totalCases;
    private int passedCases;
    private double minPassRate;
    private boolean passed;
    private String message;
}
