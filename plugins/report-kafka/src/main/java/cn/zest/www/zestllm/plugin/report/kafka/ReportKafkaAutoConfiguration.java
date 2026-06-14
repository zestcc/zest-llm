package cn.zest.www.zestllm.plugin.report.kafka;

import cn.zest.www.zestllm.spi.report.ReportChannelAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@EnableConfigurationProperties(KafkaReportProperties.class)
public class ReportKafkaAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.report-channel", havingValue = "kafka")
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnMissingBean(ReportChannelAdapter.class)
    public ReportChannelAdapter kafkaReportChannelAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                                          KafkaReportProperties properties,
                                                          ObjectMapper objectMapper) {
        return new KafkaReportChannelAdapter(kafkaTemplate, properties, objectMapper);
    }
}
