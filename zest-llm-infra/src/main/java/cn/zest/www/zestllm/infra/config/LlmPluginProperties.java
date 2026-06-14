package cn.zest.www.zestllm.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.plugins")
public class LlmPluginProperties {

    /**
     * 外置适配器 JAR 目录（*.jar），首次放入需重启进程加载 ClassLoader。
     */
    private String externalDir = "";

    /**
     * 是否在启动时扫描外置目录。
     */
    private boolean scanOnStartup = true;
}
