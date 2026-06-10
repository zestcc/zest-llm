package cn.zest.www.zestllm.spi.learning;

import cn.zest.www.zestllm.spi.model.HealthStatus;

import java.util.List;

/**
 * 自我改进闭环 SPI：Eval 跑批、失败样本建议，不训练模型权重。
 */
public interface LearningPipelineAdapter {

    String adapterId();

    LearningCycleResult runCycle(LearningCycleRequest request);

    List<EvalCaseSuggestion> suggestCasesFromTraces(TraceSampleQuery query);

    HealthStatus health();
}
