package cn.zest.www.zestllm.admin.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 适配器插件目录元数据（含未安装项与未来扩展）。
 */
@Data
@Builder
public class AdapterCatalogEntry {

    private String pluginId;
    private String pluginName;
    /** SPI 槽位，如 model-gateway、knowledge-retrieval */
    private String spiType;
    private String description;
    private String vendor;
    private String version;
    private String configProperty;
    private String configExample;
    private String mavenArtifact;
    private boolean builtIn;
    private boolean installed;
    private List<String> prerequisites;
    private List<String> relatedTemplates;
    private List<AdapterIntegrationStep> integrationSteps;

    public String catalogKey() {
        return spiType + ":" + pluginId;
    }
}
