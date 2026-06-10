package cn.zest.www.zestllm.common.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningLoopSummary {
    private boolean enabled;
    private String evalDatasetRef;
    private double minPassRate;
    private boolean probeBeforePublish;
    private boolean reviewRequired;
}
