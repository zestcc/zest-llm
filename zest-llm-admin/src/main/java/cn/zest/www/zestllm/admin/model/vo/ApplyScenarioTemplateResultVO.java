package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplyScenarioTemplateResultVO {
    private String taskCode;
    private String profileVersion;
    private boolean published;
    private String message;
}
