package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import cn.zest.www.zestllm.admin.model.request.CreateGatewayModelRequest;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.admin.repo.LlmSecretRefRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelRegistryManageServiceTest {

    @Mock
    private LlmGatewayModelRepo gatewayModelRepo;
    @Mock
    private LlmSecretRefRepo secretRefRepo;
    @Mock
    private AuditService auditService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ModelRegistryManageService modelRegistryManageService;

    @Test
    void create_rejectsInvalidModelName() {
        CreateGatewayModelRequest request = new CreateGatewayModelRequest();
        request.setModelName("bad name!");
        request.setUpstreamModel("openai/gpt-4o-mini");

        assertThatThrownBy(() -> modelRegistryManageService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("model_name");
    }

    @Test
    void create_persistsValidModel() {
        CreateGatewayModelRequest request = new CreateGatewayModelRequest();
        request.setModelName("deepseek-v4-flash");
        request.setUpstreamModel("deepseek/deepseek-v4-flash");

        when(gatewayModelRepo.findByModelName("deepseek-v4-flash")).thenReturn(Optional.empty());

        var vo = modelRegistryManageService.create(request);

        assertThat(vo.getModelName()).isEqualTo("deepseek-v4-flash");
        assertThat(vo.getSyncStatus()).isEqualTo("PENDING");
        verify(gatewayModelRepo).insert(any(LlmGatewayModelDO.class));
    }
}
