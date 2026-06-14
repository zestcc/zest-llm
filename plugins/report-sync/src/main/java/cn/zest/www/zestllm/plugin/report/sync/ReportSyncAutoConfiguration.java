package cn.zest.www.zestllm.plugin.report.sync;

import cn.zest.www.zestllm.spi.report.ReportChannelAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ReportSyncAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zest.llm.adapters.report-channel", havingValue = "sync", matchIfMissing = true)
    @ConditionalOnMissingBean(ReportChannelAdapter.class)
    public ReportChannelAdapter syncReportChannelAdapter() {
        return new SyncReportChannelAdapter();
    }
}
