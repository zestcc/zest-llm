package cn.zest.www.zestllm.infra.plugin;

import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 外置 SPI 适配器注册表（对齐 zest-monitor PluginRuntimeRegistry 的发现层）。
 */
@Slf4j
@Component
public class ExternalAdapterRegistry {

    public static final String SOURCE_EXTERNAL = "EXTERNAL";

    private final List<ExternalAdapterDescriptor> descriptors = new CopyOnWriteArrayList<>();
    private final Map<String, Object> instanceCache = new ConcurrentHashMap<>();

    public void register(ExternalAdapterDescriptor descriptor) {
        descriptors.removeIf(item -> item.getCatalogKey().equals(descriptor.getCatalogKey()));
        descriptors.add(descriptor);
        instanceCache.remove(descriptor.getCatalogKey());
        log.info("已注册外置适配器: {} from {}", descriptor.getCatalogKey(), descriptor.getJarFileName());
    }

    public void clear() {
        descriptors.clear();
        instanceCache.clear();
        log.info("外置适配器注册表已清空");
    }

    public List<ExternalAdapterDescriptor> listAll() {
        return List.copyOf(descriptors);
    }

    public boolean isRegistered(String spiType, String pluginId) {
        return descriptors.stream().anyMatch(item -> item.matches(spiType, pluginId));
    }

    public Optional<KnowledgeRetrievalAdapter> knowledge(String pluginId) {
        return resolve("knowledge-retrieval", pluginId, KnowledgeRetrievalAdapter.class);
    }

    public Optional<ModelGatewayAdapter> modelGateway(String pluginId) {
        return resolve("model-gateway", pluginId, ModelGatewayAdapter.class);
    }

    public Optional<ObservabilityAdapter> observability(String pluginId) {
        return resolve("observability", pluginId, ObservabilityAdapter.class);
    }

    public Optional<AgentRuntimeAdapter> agentRuntime(String pluginId) {
        return resolve("agent-runtime", pluginId, AgentRuntimeAdapter.class);
    }

    public Optional<LearningPipelineAdapter> learningPipeline(String pluginId) {
        return resolve("learning-pipeline", pluginId, LearningPipelineAdapter.class);
    }

    public List<Map<String, Object>> listViews() {
        List<Map<String, Object>> views = new ArrayList<>();
        for (ExternalAdapterDescriptor descriptor : descriptors) {
            views.add(Map.of(
                    "catalogKey", descriptor.getCatalogKey(),
                    "spiType", descriptor.getSpiType(),
                    "pluginId", descriptor.getPluginId(),
                    "pluginName", descriptor.getPluginName(),
                    "source", descriptor.getSource(),
                    "jarFileName", descriptor.getJarFileName() == null ? "" : descriptor.getJarFileName()
            ));
        }
        return views;
    }

    private <T> Optional<T> resolve(String spiType, String pluginId, Class<T> type) {
        return descriptors.stream()
                .filter(item -> item.matches(spiType, pluginId))
                .findFirst()
                .map(item -> type.cast(instanceCache.computeIfAbsent(item.getCatalogKey(),
                        key -> item.getInstanceSupplier().get())));
    }
}
