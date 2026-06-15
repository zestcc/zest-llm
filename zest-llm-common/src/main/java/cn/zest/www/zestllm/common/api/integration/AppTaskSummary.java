package cn.zest.www.zestllm.common.api.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * App 下 AI 作业运行时摘要（已发布 Profile、模型路由、最近探测）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppTaskSummary {
    private String taskCode;
    private String taskName;
    private String taskStatus;
    private String publishedVersion;
    private String profileStatus;
    private String runtimeMode;
    private String providerPresetCode;
    private String primaryModel;
    private String overallStatus;
    private boolean ready;
    private LocalDateTime lastProbedAt;
}
