package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest-llm.admin.execution-archive")
public class ExecutionArchiveProperties {
    private boolean enabled = false;
    private int retentionDays = 90;
    /** cron 默认每天 03:00 */
    private String cron = "0 0 3 * * ?";
}
