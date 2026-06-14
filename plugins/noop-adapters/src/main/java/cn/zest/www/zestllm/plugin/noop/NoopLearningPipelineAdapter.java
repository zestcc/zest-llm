package cn.zest.www.zestllm.plugin.noop;

import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.LearningCycleRequest;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import cn.zest.www.zestllm.spi.model.HealthStatus;

import java.util.Collections;
import java.util.List;

public class NoopLearningPipelineAdapter implements LearningPipelineAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public LearningCycleResult runCycle(LearningCycleRequest request) {
        boolean enabled = request.getLearningLoop() != null && request.getLearningLoop().isEnabled();
        return LearningCycleResult.builder()
                .passRate(1.0)
                .totalCases(0)
                .passedCases(0)
                .probePassed(true)
                .publishAllowed(!enabled || request.isDryRun())
                .message(enabled ? "learning loop enabled but adapter is noop" : "learning loop disabled")
                .build();
    }

    @Override
    public List<EvalCaseSuggestion> suggestCasesFromTraces(TraceSampleQuery query) {
        return Collections.emptyList();
    }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("noop learning pipeline").build();
    }
}
