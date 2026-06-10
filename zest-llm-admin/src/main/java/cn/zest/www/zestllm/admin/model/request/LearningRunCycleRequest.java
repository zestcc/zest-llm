package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class LearningRunCycleRequest {
    private String taskCode;
    private String profileVersion;
    private boolean dryRun = true;
}
