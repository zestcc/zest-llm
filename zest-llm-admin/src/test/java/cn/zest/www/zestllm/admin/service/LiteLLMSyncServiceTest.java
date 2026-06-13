package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.infra.config.LiteLLMProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiteLLMSyncServiceTest {

    @Mock
    private LlmGatewayModelRepo gatewayModelRepo;
    @Mock
    private SecretRefManageService secretRefManageService;

    @Test
    void buildModelPayload_includesUpstreamModelAndApiKey() {
        when(secretRefManageService.resolveLiteLLMApiKey("deepseek-api-key"))
                .thenReturn("os.environ/DEEPSEEK_API_KEY");

        LiteLLMSyncService service = new LiteLLMSyncService(
                gatewayModelRepo, secretRefManageService, new LiteLLMProperties(), new ObjectMapper());

        LlmGatewayModelDO model = new LlmGatewayModelDO();
        model.setModelName("deepseek-v4-flash");
        model.setUpstreamModel("deepseek/deepseek-v4-flash");
        model.setApiKeySecretRef("deepseek-api-key");

        var payload = service.buildModelPayload(model);

        assertThat(payload.get("model_name").asText()).isEqualTo("deepseek-v4-flash");
        assertThat(payload.get("litellm_params").get("model").asText()).isEqualTo("deepseek/deepseek-v4-flash");
        assertThat(payload.get("litellm_params").get("api_key").asText()).isEqualTo("os.environ/DEEPSEEK_API_KEY");
    }

    @Test
    void syncAll_marksFailedWhenLiteLLMUnreachable() {
        LiteLLMProperties properties = new LiteLLMProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setConnectTimeoutMs(500);
        properties.setReadTimeoutMs(500);

        LlmGatewayModelDO model = new LlmGatewayModelDO();
        model.setModelName("deepseek-v4-flash");
        model.setUpstreamModel("deepseek/deepseek-v4-flash");
        when(gatewayModelRepo.findAllActive()).thenReturn(List.of(model));

        LiteLLMSyncService service = new LiteLLMSyncService(
                gatewayModelRepo, secretRefManageService, properties, new ObjectMapper());
        var result = service.syncAll();

        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getSynced()).isZero();
    }

    @Test
    void getSyncStatus_reportsModelCounts() {
        LiteLLMProperties properties = new LiteLLMProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setConnectTimeoutMs(500);
        properties.setReadTimeoutMs(500);

        LlmGatewayModelDO synced = new LlmGatewayModelDO();
        synced.setModelName("deepseek-v4-flash");
        synced.setUpstreamModel("deepseek/deepseek-v4-flash");
        synced.setSyncStatus("SYNCED");
        when(gatewayModelRepo.findAllActive()).thenReturn(List.of(synced));

        LiteLLMSyncService service = new LiteLLMSyncService(
                gatewayModelRepo, secretRefManageService, properties, new ObjectMapper());
        var status = service.getSyncStatus();

        assertThat(status.getTotal()).isEqualTo(1);
        assertThat(status.getSynced()).isEqualTo(1);
        assertThat(status.isLiteLLMReachable()).isFalse();
        assertThat(status.getModels()).hasSize(1);
        assertThat(status.getModels().get(0).getModelName()).isEqualTo("deepseek-v4-flash");
    }
}
