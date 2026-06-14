package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdapterCatalogPageVO {
    private String profile;
    private Map<String, Object> summary;
    private List<AdapterCatalogItemVO> plugins;
    private Map<String, String> activeDefaults;
    private Map<String, String> runtimeOverrides;
    private String externalDir;
    private List<Map<String, Object>> externalPlugins;
}
