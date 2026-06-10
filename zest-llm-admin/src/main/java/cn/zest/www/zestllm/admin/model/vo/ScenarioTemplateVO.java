package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScenarioTemplateVO {
    private String id;
    private String name;
    private String description;
    private String recommendedTier;
    private String taskCodeSuggestion;
    private String taskName;
    private String runtimeMode;
    private boolean requiresMcp;
    private boolean requiresKnowledge;
}
