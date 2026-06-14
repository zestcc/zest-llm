package cn.zest.www.zestllm.infra.plugin;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.function.Supplier;

@Data
@Builder
public class ExternalAdapterDescriptor {

    private String spiType;
    private String pluginId;
    private String pluginName;
    private String source;
    private String jarFileName;
    private Path jarPath;
    private Supplier<Object> instanceSupplier;
    private Class<?> adapterClass;

    public String getCatalogKey() {
        return spiType + ":" + pluginId;
    }

    public boolean matches(String type, String id) {
        return spiType.equals(type) && pluginId.equals(id);
    }
}
