package cn.zest.www.zestllm.common.api.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 集成状态检查项（与 Admin Probe 检查项结构对齐，供第三方 App 消费）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppIntegrationCheck {
    private String name;
    private String category;
    private boolean critical;
    private boolean up;
    private String message;
}
