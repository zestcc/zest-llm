package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.infra.config.LlmPluginProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterLoader;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalAdapterAdminService {

    private final ExternalAdapterLoader externalAdapterLoader;
    private final ExternalAdapterRegistry externalAdapterRegistry;
    private final LlmPluginProperties pluginProperties;

    public int rescanExternalPlugins() {
        return externalAdapterLoader.rescan(pluginProperties.getExternalDir());
    }

    public List<Map<String, Object>> listExternalPlugins() {
        return externalAdapterRegistry.listViews();
    }
}
