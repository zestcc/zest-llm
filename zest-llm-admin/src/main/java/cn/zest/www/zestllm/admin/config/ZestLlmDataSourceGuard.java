package cn.zest.www.zestllm.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 启动门禁：禁止 H2，仅允许 MySQL 等业务库。
 */
@Slf4j
@Component
@ConditionalOnBean(DataSource.class)
@RequiredArgsConstructor
public class ZestLlmDataSourceGuard implements ApplicationListener<ApplicationReadyEvent> {

    private final DataSource dataSource;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            String lower = url.toLowerCase();
            if (lower.contains("jdbc:h2:") || lower.contains(":h2:")) {
                throw new IllegalStateException(
                        "检测到 H2 数据源，本项目仅支持 MySQL。请配置 spring.datasource.url=jdbc:mysql://...");
            }
            if (!lower.contains("jdbc:mysql:")) {
                throw new IllegalStateException(
                        "不支持的数据库类型: " + url + "，请使用 MySQL 8");
            }
            log.info("数据源校验通过（MySQL）");
        } catch (SQLException ex) {
            throw new IllegalStateException("数据源不可用，请检查 MySQL 连接配置", ex);
        }
    }
}
