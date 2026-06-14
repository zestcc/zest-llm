package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdapterCatalogItemVO {
    private String catalogKey;
    private String pluginId;
    private String pluginName;
    private String spiType;
    private String description;
    private String vendor;
    private String version;
    private String loadStatus;
    private boolean active;
    private boolean installed;
    private boolean builtIn;
    private boolean external;
    private boolean healthUp;
    private String healthMessage;
}
