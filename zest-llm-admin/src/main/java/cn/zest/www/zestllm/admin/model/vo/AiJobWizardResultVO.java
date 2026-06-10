package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiJobWizardResultVO {
    private String taskCode;
    private String profileVersion;
    private boolean published;
    private String probeStatus;
    private String scenarioName;
    private String recommendedTier;
    private String nextUrl;
    private String message;
}
