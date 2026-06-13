package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import cn.zest.www.zestllm.admin.model.request.CreateGatewayModelRequest;
import cn.zest.www.zestllm.admin.model.request.CreateProviderPresetRequest;
import cn.zest.www.zestllm.admin.model.request.IntegrationImportGatewayModelsRequest;
import cn.zest.www.zestllm.admin.model.request.IntegrationImportProviderPresetsRequest;
import cn.zest.www.zestllm.admin.model.vo.GatewayModelVO;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.admin.repo.LlmProviderPresetRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationImportServiceTest {

    @Mock
    private ProviderPresetManageService providerPresetManageService;
    @Mock
    private LlmProviderPresetRepo providerPresetRepo;
    @Mock
    private AgentProfileManageService agentProfileManageService;
    @Mock
    private ModelRegistryManageService modelRegistryManageService;
    @Mock
    private LlmGatewayModelRepo gatewayModelRepo;

    @InjectMocks
    private IntegrationImportService integrationImportService;

    @Test
    void importProviderPresets_idempotentByPresetCode() {
        CreateProviderPresetRequest item = new CreateProviderPresetRequest();
        item.setPresetCode("litellm-default");
        item.setPresetName("LiteLLM");
        item.setConfigJson("{}");

        IntegrationImportProviderPresetsRequest request = new IntegrationImportProviderPresetsRequest();
        request.setItems(List.of(item));

        when(providerPresetRepo.findByCode("litellm-default")).thenReturn(Optional.of(new cn.zest.www.zestllm.admin.model.entity.LlmProviderPresetDO()));

        var result = integrationImportService.importProviderPresets(request);

        assertThat(result.getUpdated()).isEqualTo(1);
        verify(providerPresetManageService).update(eq("litellm-default"), any());
        verify(providerPresetManageService, never()).create(any());
    }

    @Test
    void importGatewayModels_upsertsExisting() {
        CreateGatewayModelRequest item = new CreateGatewayModelRequest();
        item.setModelName("deepseek-v4-flash");
        item.setUpstreamModel("deepseek/deepseek-v4-flash");

        IntegrationImportGatewayModelsRequest request = new IntegrationImportGatewayModelsRequest();
        request.setItems(List.of(item));

        when(gatewayModelRepo.findByModelName("deepseek-v4-flash")).thenReturn(Optional.of(new LlmGatewayModelDO()));
        when(modelRegistryManageService.upsertForImport(item)).thenReturn(GatewayModelVO.builder().modelName("deepseek-v4-flash").build());

        var result = integrationImportService.importGatewayModels(request);

        assertThat(result.getUpdated()).isEqualTo(1);
        verify(modelRegistryManageService).upsertForImport(item);
    }
}
