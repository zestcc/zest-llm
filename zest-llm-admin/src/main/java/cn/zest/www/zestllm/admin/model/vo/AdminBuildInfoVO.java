package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminBuildInfoVO {
    private String appVersion;
    private String artifactId;
    private String flywayLatestScript;
    private String activeProfiles;
    private String javaVersion;
}
