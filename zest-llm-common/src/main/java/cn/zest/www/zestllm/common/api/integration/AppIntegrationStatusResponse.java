package cn.zest.www.zestllm.common.api.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 第三方 App 集成状态（SSOT：由 ZestLLM 控制面聚合返回）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppIntegrationStatusResponse {
    private String appKey;
    private String appName;
    private String appStatus;
    /** READY | DEGRADED | UNAVAILABLE */
    private String overallStatus;
    private boolean ready;
    private int taskCount;
    private int readyTaskCount;
    private List<AppTaskSummary> tasks;
    private LocalDateTime checkedAt;
}
