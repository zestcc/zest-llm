package cn.zest.www.zestllm.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zest-llm.admin.observability")
public class AdminObservabilityProperties {
    /** Langfuse UI 基址（可与 ingest API 不同，如 http://localhost:3000） */
    private String langfuseUiBaseUrl = "http://localhost:3000";
    /** Trace 详情路径模板，{traceId} 占位 */
    private String tracePathTemplate = "/trace/{traceId}";
}
