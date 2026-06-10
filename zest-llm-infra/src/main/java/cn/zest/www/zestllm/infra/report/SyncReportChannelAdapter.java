package cn.zest.www.zestllm.infra.report;

import cn.zest.www.zestllm.spi.report.ReportChannelAdapter;
import cn.zest.www.zestllm.spi.report.ReportDelivery;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认同步通道：Execution 已由 CP 落库，此处仅作 SPI 边界与扩展点。
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
