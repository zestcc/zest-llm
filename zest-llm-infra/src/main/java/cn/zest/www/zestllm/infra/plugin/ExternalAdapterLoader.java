package cn.zest.www.zestllm.infra.plugin;

import cn.zest.www.zestllm.infra.config.LlmPluginProperties;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.runtime.AgentRuntimeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * 扫描外置插件目录，通过 Java SPI 注册适配器（参考 zest-monitor ExternalPluginLoader）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAdapterLoader implements ApplicationRunner {

    private final LlmPluginProperties pluginProperties;
    private final ExternalAdapterRegistry registry;

    @Override
    public void run(ApplicationArguments args) {
        if (!pluginProperties.isScanOnStartup()) {
            log.info("外置适配器扫描已禁用");
            return;
        }
        scanDirectory(pluginProperties.getExternalDir());
    }

    public int rescan(String directory) {
        registry.clear();
        return scanDirectory(directory);
    }

    private int scanDirectory(String dir) {
        if (!StringUtils.hasText(dir)) {
            log.debug("未配置 zest.llm.plugins.external-dir，跳过外置适配器扫描");
            return 0;
        }
        Path pluginDir = Path.of(dir.trim());
        if (!Files.isDirectory(pluginDir)) {
            log.warn("外置适配器目录不存在: {}", pluginDir.toAbsolutePath());
            return 0;
        }
        int loaded = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginDir, "*.jar")) {
            for (Path jar : stream) {
                loaded += loadJar(jar);
            }
        } catch (Exception ex) {
            log.error("扫描外置适配器目录失败: {}", pluginDir, ex);
        }
        log.info("外置适配器扫描完成: dir={}, loaded={}", pluginDir.toAbsolutePath(), loaded);
        return loaded;
    }

    private int loadJar(Path jarPath) {
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    ExternalAdapterLoader.class.getClassLoader());
            int count = 0;
            count += registerSpi(classLoader, jarPath, KnowledgeRetrievalAdapter.class, "knowledge-retrieval");
            count += registerSpi(classLoader, jarPath, ModelGatewayAdapter.class, "model-gateway");
            count += registerSpi(classLoader, jarPath, ObservabilityAdapter.class, "observability");
            count += registerSpi(classLoader, jarPath, AgentRuntimeAdapter.class, "agent-runtime");
            count += registerSpi(classLoader, jarPath, LearningPipelineAdapter.class, "learning-pipeline");
            return count;
        } catch (Exception ex) {
            log.error("加载外置适配器 JAR 失败: {}", jarPath, ex);
            return 0;
        }
    }

    private <T> int registerSpi(ClassLoader classLoader, Path jarPath, Class<T> spiType, String spiKind) {
        int count = 0;
        for (T prototype : ServiceLoader.load(spiType, classLoader)) {
            String pluginId = readPluginId(prototype);
            Supplier<Object> supplier = () -> newInstance(prototype.getClass());
            registry.register(ExternalAdapterDescriptor.builder()
                    .spiType(spiKind)
                    .pluginId(pluginId)
                    .pluginName(pluginId)
                    .source(ExternalAdapterRegistry.SOURCE_EXTERNAL)
                    .jarFileName(jarPath.getFileName().toString())
                    .jarPath(jarPath)
                    .adapterClass(prototype.getClass())
                    .instanceSupplier(supplier)
                    .build());
            count++;
        }
        return count;
    }

    private String readPluginId(Object adapter) {
        if (adapter instanceof KnowledgeRetrievalAdapter knowledge) {
            return knowledge.adapterId();
        }
        if (adapter instanceof ModelGatewayAdapter gateway) {
            return gateway.adapterId();
        }
        if (adapter instanceof ObservabilityAdapter observability) {
            return observability.adapterId();
        }
        if (adapter instanceof AgentRuntimeAdapter runtime) {
            return runtime.adapterId();
        }
        if (adapter instanceof LearningPipelineAdapter learning) {
            return learning.adapterId();
        }
        return adapter.getClass().getSimpleName();
    }

    private Object newInstance(Class<?> implClass) {
        try {
            return implClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("外置适配器缺少无参构造: " + implClass.getName(), ex);
        }
    }
}
