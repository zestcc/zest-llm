package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zestflow.admin")
public class ZestFlowAdminProperties {

    /**
     * Executor/Collector 注册令牌；为空时仅本地开发放行。
     */
    private String registryToken = "";

    private String deployMode = "standalone";
}
