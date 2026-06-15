package cn.zest.www.zestllm.admin.plugin;

import lombok.Builder;
import lombok.Data;

/**
 * 插件配置项说明（参考 Grafana Data Source 配置表）。
 */
@Data
@Builder
public class AdapterConfigRef {

    private String key;
    private String description;
    private boolean required;
    private String example;
    /** 对应环境变量名（若有） */
    private String envVar;
}
