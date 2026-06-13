package cn.zest.www.zestllm.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AdminOidcProperties.class, AdminSsoProperties.class, ExecutionArchiveProperties.class, AgentProfileProbeProperties.class, LearningCycleProperties.class, LearningAutoPublishProperties.class, AdminObservabilityProperties.class, ZestFlowAdminProperties.class, IntegrationWebhookProperties.class})
@RequiredArgsConstructor
public class AdminAutoConfiguration {

    private final DataSource dataSource;

    @PostConstruct
    public void validateMySqlDataSource() {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL().toLowerCase();
            if (!url.contains(":mysql:")) {
                throw new IllegalStateException(
                        "ZestLLM 仅支持 MySQL 数据源，当前 URL=" + url
                                + "。请先执行: cd deploy && docker compose up -d mysql");
            }
            log.info("DataSource validated: MySQL");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("无法校验数据源，请确认 MySQL 已启动且配置正确", ex);
        }
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
