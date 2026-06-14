package cn.zest.www.zestllm.plugin.report.sync;

import cn.zest.www.zestllm.spi.report.ReportChannelAdapter;
import cn.zest.www.zestllm.spi.report.ReportDelivery;
import lombok.extern.slf4j.Slf4j;

/**
 * Default sync channel: execution is already persisted; SPI extension point only.
 */
@Slf4j
public class SyncReportChannelAdapter implements ReportChannelAdapter {

    @Override
    public String adapterId() {
        return "sync";
    }

    @Override
    public void deliver(ReportDelivery delivery) {
        log.debug("Report delivered sync traceId={} status={}", delivery.getTraceId(), delivery.getStatus());
    }
}
