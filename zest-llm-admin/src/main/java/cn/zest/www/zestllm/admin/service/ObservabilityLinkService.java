package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.AdminObservabilityProperties;
import cn.zest.www.zestllm.admin.model.vo.ObservabilityConfigVO;
import cn.zest.www.zestllm.plugin.observability.langfuse.LangfuseProperties;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ObservabilityLinkService {

    private final LlmAdapterProperties adapterProperties;
    private final LangfuseProperties langfuseProperties;
    private final AdminObservabilityProperties adminObservabilityProperties;

    public ObservabilityConfigVO config() {
        return ObservabilityConfigVO.builder()
                .adapterId(adapterProperties.getObservability())
                .langfuseEnabled(langfuseProperties.isEnabled() && "langfuse".equals(adapterProperties.getObservability()))
                .langfuseUiBaseUrl(adminObservabilityProperties.getLangfuseUiBaseUrl())
                .tracePathTemplate(adminObservabilityProperties.getTracePathTemplate())
                .build();
    }

    public String buildTraceUrl(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return null;
        }
        if (!"langfuse".equals(adapterProperties.getObservability()) || !langfuseProperties.isEnabled()) {
            return null;
        }
        String base = adminObservabilityProperties.getLangfuseUiBaseUrl();
        if (!StringUtils.hasText(base)) {
            base = langfuseProperties.getBaseUrl();
        }
        String path = adminObservabilityProperties.getTracePathTemplate().replace("{traceId}", traceId);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base.replaceAll("/$", "") + path;
    }
}
