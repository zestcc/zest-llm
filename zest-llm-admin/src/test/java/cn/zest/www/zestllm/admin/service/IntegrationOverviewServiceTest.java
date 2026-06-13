package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import cn.zest.www.zestllm.admin.model.vo.AdapterHealthVO;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.admin.repo.LlmSecretRefRepo;
import cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationOverviewServiceTest {

    @Mock
    private LlmGatewayModelRepo gatewayModelRepo;
    @Mock
    private LlmSecretRefRepo secretRefRepo;
    @Mock
    private AdapterHealthService adapterHealthService;
    @Mock
    private ModelGatewayAdapter modelGatewayAdapter;

    @InjectMocks
    private IntegrationOverviewService integrationOverviewService;

    @Test
    void overview_aggregatesGatewayModelsAndAdapters() {
        LlmGatewayModelDO synced = new LlmGatewayModelDO();
        synced.setSyncStatus("SYNCED");
        LlmGatewayModelDO pending = new LlmGatewayModelDO();
        pending.setSyncStatus(null);

        when(gatewayModelRepo.findAllActive()).thenReturn(List.of(synced, pending));
        when(secretRefRepo.findAllActive()).thenReturn(List.of(new cn.zest.www.zestllm.admin.model.entity.LlmSecretRefDO()));
        when(adapterHealthService.listAll()).thenReturn(List.of(
                AdapterHealthVO.builder().kind("model-gateway").up(true).build(),
                AdapterHealthVO.builder().kind("knowledge-retrieval").up(false).build()));
        when(modelGatewayAdapter.health()).thenReturn(HealthStatus.builder().up(true).message("ok").build());

        var overview = integrationOverviewService.overview();

        assertThat(overview.getGatewayModels().getTotal()).isEqualTo(2);
        assertThat(overview.getGatewayModels().getSynced()).isEqualTo(1);
        assertThat(overview.getGatewayModels().getPending()).isEqualTo(1);
        assertThat(overview.getSecretRefCount()).isEqualTo(1);
        assertThat(overview.isLiteLLMReachable()).isTrue();
        assertThat(overview.getAdaptersUp()).isEqualTo(1);
        assertThat(overview.getAdaptersDown()).isEqualTo(1);
        assertThat(overview.getAdapterIssues()).hasSize(1);
    }
}
