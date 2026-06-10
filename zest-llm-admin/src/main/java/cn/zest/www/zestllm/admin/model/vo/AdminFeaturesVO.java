package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AdminFeaturesVO {
    private String appVersion;
    private String flywayLatestScript;
    private boolean agentProbeApi;
    private Map<String, Boolean> schemaReady;
}
