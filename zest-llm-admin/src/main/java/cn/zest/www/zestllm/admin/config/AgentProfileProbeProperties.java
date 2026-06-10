package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest-llm.admin.agent-probe")
public class AgentProfileProbeProperties {
    /** 定时巡检已发布 Profile */
    private boolean scheduleEnabled = true;
    /** 默认每 30 分钟 */
    private String cron = "0 */30 * * * ?";
    /** 定时巡检是否包含网关冒烟（消耗 token） */
    private boolean smokeTestOnSchedule = false;
    /** UNAVAILABLE / DEGRADED 时 Webhook 告警 URL（空则关闭） */
    private String alertWebhookUrl;
    /** 是否对 DEGRADED 也发告警（默认仅 UNAVAILABLE） */
    private boolean alertOnDegraded = false;
    /** 同一作业同一状态告警冷却（分钟） */
    private int alertCooldownMinutes = 60;
}
