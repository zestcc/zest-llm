package cn.zest.www.zestllm.plugin.identity.zestsso;

import cn.zest.sso.client.ZestSsoOidcClient;
import cn.zest.www.zestllm.spi.adminsso.AdminSsoSessionRevocation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ZestSSO Client SDK 自动配置（仅当 classpath 含 SDK 且显式启用 SSO 时加载）。
 */
@AutoConfiguration
@ConditionalOnClass(ZestSsoOidcClient.class)
@ConditionalOnProperty(prefix = "zest-llm.admin.sso", name = "enabled", havingValue = "true")
@ConditionalOnBean(AdminSsoSessionRevocation.class)
@EnableConfigurationProperties(ZestSsoPluginProperties.class)
public class IdentityZestSsoAutoConfiguration {

    @Bean
    public ZestSsoLogoutHandler zestSsoLogoutHandler(AdminSsoSessionRevocation sessionRevocation) {
        return new ZestSsoLogoutHandler(sessionRevocation);
    }

    @Bean
    public ZestSsoPropertyBridge zestSsoPropertyBridge(ZestSsoPluginProperties properties,
                                                       cn.zest.sso.client.ZestSsoClientProperties clientProperties) {
        return new ZestSsoPropertyBridge(properties, clientProperties);
    }
}
