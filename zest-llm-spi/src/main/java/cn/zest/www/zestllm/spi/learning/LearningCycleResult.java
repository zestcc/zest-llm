package cn.zest.www.zestllm.spi.learning;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class LearningCycleResult {

    private double passRate;
    private int totalCases;
    private int passedCases;
    private boolean probePassed;
    private boolean publishAllowed;
    private String message;

    @Builder.Default
    private List<String> failedCaseCodes = new ArrayList<>();
}
