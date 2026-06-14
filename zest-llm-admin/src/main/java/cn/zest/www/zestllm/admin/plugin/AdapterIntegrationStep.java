package cn.zest.www.zestllm.admin.plugin;

import lombok.Builder;
import lombok.Data;

/**
 * 插件集成引导步骤（参考 Grafana Data Source 向导 / Dify 接入流程）。
 */
@Data
@Builder
public class AdapterIntegrationStep {

    private String stepId;
    private int order;
    private String title;
    private String description;
    /** CONFIG | VERIFY | NAVIGATE | COMMAND | DOC */
    private String actionType;
    private String actionLabel;
    /** Admin 路由、配置项或文档锚点 */
    private String actionTarget;
    private String commandExample;
    private String docUrl;
    private boolean required;
}
