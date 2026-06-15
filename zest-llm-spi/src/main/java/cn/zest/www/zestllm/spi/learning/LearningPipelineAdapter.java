package cn.zest.www.zestllm.spi.learning;

import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;

import java.util.List;

/**
 * 自我改进闭环 SPI：Eval 跑批、失败样本建议，不训练模型权重。
 */
public interface LearningPipelineAdapter {

    String adapterId();

    LearningCycleResult runCycle(LearningCycleRequest request);

    List<EvalCaseSuggestion> suggestCasesFromTraces(TraceSampleQuery query);

    HealthStatus health();

    default LearningCycleResult validateForPublish(String taskCode, String version, AgentProfileDocument document) {
        LearningLoopConfig loop = ProfileExtensions.learningLoop(document).orElse(null);
        if (loop == null || !loop.isEnabled()) {
            return LearningCycleResult.builder()
                    .passRate(1.0)
                    .publishAllowed(true)
                    .probePassed(true)
                    .message("learning loop disabled")
                    .build();
        }
        return runCycle(LearningCycleRequest.builder()
                .taskCode(taskCode)
                .profileVersion(version)
                .learningLoop(loop)
                .dryRun(true)
                .build());
    }
}
