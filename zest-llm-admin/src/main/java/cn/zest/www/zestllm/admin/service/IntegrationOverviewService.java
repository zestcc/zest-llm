package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import cn.zest.www.zestllm.admin.model.vo.AdapterHealthVO;
import cn.zest.www.zestllm.admin.model.vo.IntegrationOverviewVO;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.admin.repo.LlmSecretRefRepo;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegrationOverviewService {

    private final LlmGatewayModelRepo gatewayModelRepo;
    private final LlmSecretRefRepo secretRefRepo;
    private final AdapterHealthService adapterHealthService;
    private final ModelGatewayAdapter modelGatewayAdapter;

    public IntegrationOverviewVO overview() {
        List<LlmGatewayModelDO> models = gatewayModelRepo.findAllActive();
        int synced = 0;
        int failed = 0;
        int pending = 0;
        for (LlmGatewayModelDO model : models) {
            String status = model.getSyncStatus();
            if ("SYNCED".equalsIgnoreCase(status)) {
                synced++;
            } else if ("FAILED".equalsIgnoreCase(status)) {
                failed++;
            } else {
                pending++;
            }
        }
        List<AdapterHealthVO> adapters = adapterHealthService.listAll();
        List<AdapterHealthVO> issues = adapters.stream().filter(a -> !a.isUp()).toList();
        HealthStatus gatewayHealth = modelGatewayAdapter.health();
        return IntegrationOverviewVO.builder()
                .gatewayModels(IntegrationOverviewVO.GatewayModelSummary.builder()
                        .total(models.size())
                        .synced(synced)
                        .failed(failed)
                        .pending(pending)
                        .build())
                .secretRefCount(secretRefRepo.findAllActive().size())
                .liteLLMReachable(gatewayHealth.isUp())
                .adaptersUp((int) adapters.stream().filter(AdapterHealthVO::isUp).count())
                .adaptersDown(issues.size())
                .adapterIssues(issues)
                .build();
    }
}
