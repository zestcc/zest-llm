package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
public class AdapterCatalogDetailVO {
    private String catalogKey;
    private String pluginId;
    private String pluginName;
    private String spiType;
    private String description;
    private String vendor;
    private String version;
    private String configProperty;
    private String configExample;
    private String mavenArtifact;
    private boolean builtIn;
    private boolean installed;
    private boolean active;
    private String loadStatus;
    private String configuredValue;
    private String pendingValue;
    private boolean restartRequired;
    private boolean healthUp;
    private String healthMessage;
    private List<String> prerequisites;
    private List<String> relatedTemplates;
    private List<AdapterIntegrationStepVO> integrationSteps;
    private Map<String, String> runtimeOverrides;
}
