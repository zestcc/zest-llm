package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.AdapterHealthVO;
import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.spi.audit.AuditAdapter;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.observability.ObservabilityAdapter;
import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import cn.zest.www.zestllm.spi.quota.QuotaAdapter;
import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdapterHealthService {

    private final LlmAdapterProperties adapterProperties;
    private final ModelGatewayAdapter modelGatewayAdapter;
    private final ObservabilityAdapter observabilityAdapter;
    private final PolicyCacheAdapter policyCacheAdapter;
    private final QuotaAdapter quotaAdapter;
    private final AuditAdapter auditAdapter;
    private final PromptRenderer promptRenderer;
    private final OutputSchemaValidator outputSchemaValidator;

    public List<AdapterHealthVO> listAll() {
        List<AdapterHealthVO> items = new ArrayList<>();
        items.add(fromHealth("model-gateway", adapterProperties.getModelGateway(),
                modelGatewayAdapter.adapterId(), modelGatewayAdapter.health()));
        items.add(fromObservability());
        items.add(fromHealth("policy-cache", adapterProperties.getPolicyCache(),
                policyCacheAdapter.adapterId(), HealthStatus.builder().up(true).message("ok").build()));
        items.add(fromHealth("quota", adapterProperties.getQuota(),
                quotaAdapter.adapterId(), quotaAdapter.health()));
        items.add(fromHealth("audit", adapterProperties.getAudit(),
                auditAdapter.adapterId(), HealthStatus.builder().up(true).message("ok").build()));
        items.add(fromHealth("prompt-renderer", adapterProperties.getPromptRenderer(),
                promptRenderer.rendererId(), HealthStatus.builder().up(true).message("ok").build()));
        items.add(fromHealth("output-schema-validator", adapterProperties.getOutputSchemaValidator(),
                outputSchemaValidator.adapterId(), HealthStatus.builder().up(true).message("ok").build()));
        return items;
    }

    public AdapterHealthVO gatewayHealth() {
        HealthStatus status = modelGatewayAdapter.health();
        return AdapterHealthVO.builder()
                .adapterId(modelGatewayAdapter.adapterId())
                .up(status.isUp())
                .message(status.getMessage())
                .build();
    }

    private AdapterHealthVO fromObservability() {
        boolean up = true;
        String message = observabilityAdapter.adapterId();
        if ("langfuse".equals(observabilityAdapter.adapterId())) {
            message = "langfuse async ingest";
        }
        return AdapterHealthVO.builder()
                .kind("observability")
                .configured(adapterProperties.getObservability())
                .adapterId(observabilityAdapter.adapterId())
                .up(up)
                .message(message)
                .build();
    }

    private AdapterHealthVO fromHealth(String kind, String configured, String adapterId, HealthStatus status) {
        return AdapterHealthVO.builder()
                .kind(kind)
                .configured(configured)
                .adapterId(adapterId)
                .up(status.isUp())
                .message(status.getMessage())
                .build();
    }
}
