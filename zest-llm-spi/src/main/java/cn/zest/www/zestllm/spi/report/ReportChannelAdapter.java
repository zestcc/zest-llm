package cn.zest.www.zestllm.spi.report;

/**
 * 执行结果上报通道（同步 DB / 异步 MQ 等可插拔）。
 */
public interface ReportChannelAdapter {

    String adapterId();

    void deliver(ReportDelivery delivery);
}
