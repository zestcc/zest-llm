package cn.zest.www.zestllm.common.api.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个 AI 作业可用性探测结果（runtime token 可调用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppTaskAvailabilityResponse {
    private String appKey;
    private String taskCode;
    private String profileVersion;
    private String profileStatus;
    /** READY | DEGRADED | UNAVAILABLE */
    private String overallStatus;
    private boolean ready;
    private long latencyMs;
    private boolean smokeTest;
    private List<AppIntegrationCheck> checks;
    private LocalDateTime probedAt;
}
