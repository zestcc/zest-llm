package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest.llm.learning.auto-publish")
public class LearningAutoPublishProperties {

    /** Learning Cycle 评估通过后是否自动发布 Profile（默认关闭，需配合非 dry-run） */
    private boolean enabled = false;

    /** enabled=true 时仅记录审计日志，不调用 publish（预发/人工复核阶段） */
    private boolean auditOnly = false;
}
