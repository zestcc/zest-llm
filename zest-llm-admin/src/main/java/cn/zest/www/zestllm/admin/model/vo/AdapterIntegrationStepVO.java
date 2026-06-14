package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdapterIntegrationStepVO {
    private String stepId;
    private int order;
    private String title;
    private String description;
    private String actionType;
    private String actionLabel;
    private String actionTarget;
    private String commandExample;
    private String docUrl;
    private boolean required;
}
