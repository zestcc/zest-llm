package cn.zest.www.zestllm.agent.report;

import cn.zest.www.zestllm.common.api.ReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 侧 report 失败补偿队列：CP 短暂不可用时本地缓冲，恢复后重试。
 */
@Slf4j
public class AgentReportRetryQueue {

    private static final int MAX_ATTEMPTS = 8;
    private static final int MAX_QUEUE_SIZE = 500;

    private final RestClient controlPlaneClient;
    private final ConcurrentLinkedQueue<PendingReport> queue = new ConcurrentLinkedQueue<>();

    public AgentReportRetryQueue(RestClient controlPlaneClient) {
        this.controlPlaneClient = controlPlaneClient;
    }

    public void enqueue(ReportRequest report) {
        if (report == null || report.getTraceId() == null) {
            return;
        }
        if (queue.size() >= MAX_QUEUE_SIZE) {
            log.warn("Agent report retry queue full, dropping traceId={}", report.getTraceId());
            return;
        }
        queue.offer(new PendingReport(report));
        log.debug("Enqueued report retry traceId={}", report.getTraceId());
    }

    @Scheduled(fixedDelayString = "${zest.llm.agent.report-retry-interval-ms:30000}")
    public void flush() {
        Iterator<PendingReport> it = queue.iterator();
        while (it.hasNext()) {
            PendingReport pending = it.next();
            if (pending.attempts.incrementAndGet() > MAX_ATTEMPTS) {
                it.remove();
                log.warn("Agent report retry exhausted traceId={}", pending.report.getTraceId());
                continue;
            }
            try {
                controlPlaneClient.post()
                        .uri("/v1/llm/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(pending.report)
                        .retrieve()
                        .toBodilessEntity();
                it.remove();
                log.info("Agent report retry succeeded traceId={}", pending.report.getTraceId());
            } catch (Exception ex) {
                log.debug("Agent report retry pending traceId={} attempt={}",
                        pending.report.getTraceId(), pending.attempts.get());
            }
        }
    }

    private static final class PendingReport {
        private final ReportRequest report;
        private final AtomicInteger attempts = new AtomicInteger(0);

        private PendingReport(ReportRequest report) {
            this.report = report;
        }
    }
}
