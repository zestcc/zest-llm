package cn.zest.www.zestllm.infra.plugin;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * SPI 路由公共逻辑：外置 JAR 优先，其次匹配 adapterId 的内置 Bean。
 */
public final class RoutingAdapterSupport {

    private RoutingAdapterSupport() {
    }

    public static <T> T resolve(String configuredId,
                                  Optional<T> external,
                                  List<T> builtInDelegates,
                                  Class<?> routingType,
                                  Function<T, String> idExtractor,
                                  String spiLabel) {
        List<T> delegates = builtInDelegates.stream()
                .filter(item -> !routingType.isInstance(item))
                .toList();
        return external
                .or(() -> delegates.stream().filter(item -> configuredId.equals(idExtractor.apply(item))).findFirst())
                .or(() -> delegates.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("无可用 " + spiLabel + ": " + configuredId));
    }
}
