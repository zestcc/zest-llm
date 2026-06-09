package cn.zest.www.zestllm.spi.observability;

import cn.zest.www.zestllm.spi.model.TraceEndEvent;
import cn.zest.www.zestllm.spi.model.TraceStartEvent;

/**
 * 可观测 SPI（默认 Langfuse，可替换 OTel / Noop）。
 */
public interface ObservabilityAdapter {

    String adapterId();

    void traceStart(TraceStartEvent event);

    void traceEnd(TraceEndEvent event);
}
