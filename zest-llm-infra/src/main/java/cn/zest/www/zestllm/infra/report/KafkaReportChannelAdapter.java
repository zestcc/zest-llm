package cn.zest.www.zestllm.infra.report;

import cn.zest.www.zestllm.infra.config.KafkaReportProperties;
import cn.zest.www.zestllm.spi.report.ReportChannelAdapter;
import cn.zest.www.zestllm.spi.report.ReportDelivery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 异步 Kafka 上报通道：Execution 已落库后扇出到 MQ。
 */
@Slf4j
public class KafkaReportChannelAdapter implements ReportChannelAdapter {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaReportProperties properties;
    private final ObjectMapper objectMapper;

    public KafkaReportChannelAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                     KafkaReportProperties properties,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String adapterId() {
        return "kafka";
    }

    @Override
    public void deliver(ReportDelivery delivery) {
        try {
            String payload = objectMapper.writeValueAsString(delivery);
            kafkaTemplate.send(new ProducerRecord<>(properties.getTopic(), delivery.getTraceId(), payload));
            log.debug("Report sent to Kafka topic={} traceId={}", properties.getTopic(), delivery.getTraceId());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize report delivery traceId={}", delivery.getTraceId(), ex);
        } catch (Exception ex) {
            log.warn("Failed to send report to Kafka traceId={}", delivery.getTraceId(), ex);
        }
    }
}
