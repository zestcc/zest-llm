package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAdapterConfigDO;
import cn.zest.www.zestllm.admin.plugin.AdapterEnablementChecker;
import cn.zest.www.zestllm.admin.repo.LlmAdapterConfigRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdapterConfigService {

    private final LlmAdapterConfigRepo adapterConfigRepo;
    private final AdapterEnablementChecker enablementChecker;

    @PostConstruct
    public void loadOverrides() {
        refreshChecker();
    }

    public void refreshChecker() {
        Map<String, String> defaults = new HashMap<>();
        for (LlmAdapterConfigDO row : adapterConfigRepo.findAll()) {
            if (row.getConfigKey() != null && row.getConfigKey().startsWith(AdapterEnablementChecker.DEFAULT_KEY_PREFIX)) {
                defaults.put(row.getConfigKey(), row.getPluginId());
            }
        }
        enablementChecker.applyDefaultOverrides(defaults);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAdapter(String spiType, String pluginId) {
        String configKey = AdapterEnablementChecker.defaultKey(spiType);
        LlmAdapterConfigDO existing = adapterConfigRepo.findByConfigKey(configKey).orElse(null);
        if (existing == null) {
            LlmAdapterConfigDO row = new LlmAdapterConfigDO();
            row.setConfigKey(configKey);
            row.setSpiType(spiType);
            row.setPluginId(pluginId);
            row.setEnabled(1);
            adapterConfigRepo.insert(row);
        } else {
            existing.setPluginId(pluginId);
            existing.setEnabled(1);
            adapterConfigRepo.update(existing);
        }
        refreshChecker();
    }

    public Map<String, String> listOverrides() {
        Map<String, String> result = new HashMap<>();
        adapterConfigRepo.findAll().forEach(row -> result.put(row.getConfigKey(), row.getPluginId()));
        return result;
    }

    public String pendingPluginId(String spiType) {
        return adapterConfigRepo.findByConfigKey(AdapterEnablementChecker.defaultKey(spiType))
                .map(LlmAdapterConfigDO::getPluginId)
                .orElse(null);
    }
}
