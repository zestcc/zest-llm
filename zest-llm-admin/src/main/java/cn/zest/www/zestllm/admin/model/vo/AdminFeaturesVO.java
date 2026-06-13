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
    private boolean learningApi;
    private boolean capabilityStackApi;
    private boolean scenarioTemplateApi;
    private boolean integrationAdaptersEnabled;
    private boolean integrationSuiteApi;
    private boolean gatewayModelApi;
    private boolean secretRefApi;
    private boolean integrationImportApi;
    private Map<String, Boolean> schemaReady;
}
