package cn.zest.www.zestllm.admin.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 ZestSSO Client SDK（Back-Channel Logout 接收等）。
 */
@Configuration
@ConditionalOnProperty(prefix = "zest-llm.admin.sso", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AdminSsoProperties.class)
public class ZestLlmSsoSdkConfiguration {
}
