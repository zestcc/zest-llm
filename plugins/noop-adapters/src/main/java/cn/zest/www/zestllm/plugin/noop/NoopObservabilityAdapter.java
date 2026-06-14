package cn.zest.www.zestllm.plugin.noop;

import cn.zest.www.zestllm.spi.model.TraceEndEvent;
import cn.zest.www.zestllm.spi.model.TraceStartEvent;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopObservabilityAdapter implements ObservabilityAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public void traceStart(TraceStartEvent event) {
        log.debug("traceStart traceId={} code={}", event.getTraceId(), event.getCode());
    }

    @Override
    public void traceEnd(TraceEndEvent event) {
        log.debug("traceEnd traceId={} success={}", event.getTraceId(), event.isSuccess());
    }
}
