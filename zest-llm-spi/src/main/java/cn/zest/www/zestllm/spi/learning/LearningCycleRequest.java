package cn.zest.www.zestllm.spi.learning;

import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningCycleRequest {

    private String taskCode;
    private String profileVersion;
    private LearningLoopConfig learningLoop;
    private boolean dryRun;
}
