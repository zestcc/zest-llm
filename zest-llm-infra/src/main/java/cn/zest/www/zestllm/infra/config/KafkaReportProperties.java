package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.kafka")
public class KafkaReportProperties {
    private boolean enabled = false;
    private String topic = "zest-llm.execution.report";
    private String bootstrapServers = "localhost:9092";
}
