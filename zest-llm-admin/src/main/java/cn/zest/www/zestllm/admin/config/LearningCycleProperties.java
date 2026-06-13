package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest-llm.admin.learning-cycle")
public class LearningCycleProperties {

    /** 定时对已发布且启用 learningLoop 的 Profile 跑闭环（默认 dry-run，不自动 publish） */
    private boolean scheduleEnabled = false;

    /** 默认每天 03:00 */
    private String cron = "0 0 3 * * ?";

    /** 定时任务仅评估，不自动发布 */
    private boolean dryRunOnSchedule = true;
}
