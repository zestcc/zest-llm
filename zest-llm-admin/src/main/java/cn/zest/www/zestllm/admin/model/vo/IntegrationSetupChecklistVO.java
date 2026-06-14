package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IntegrationSetupChecklistVO {
    private int totalSteps;
    private int completedSteps;
    private int progressPercent;
    private boolean readyForProduction;
    private List<IntegrationSetupStepVO> steps;

    @Data
    @Builder
    public static class IntegrationSetupStepVO {
        private String stepId;
        private String title;
        private String description;
        private String status;
        private boolean required;
        private String navigateTo;
        private String hint;
    }
}
