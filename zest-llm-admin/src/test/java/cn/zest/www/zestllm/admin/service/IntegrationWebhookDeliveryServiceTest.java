package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmIntegrationWebhookDeliveryDO;
import cn.zest.www.zestllm.admin.repo.LlmIntegrationWebhookDeliveryRepo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationWebhookDeliveryServiceTest {

    @Mock
    private LlmIntegrationWebhookDeliveryRepo deliveryRepo;
    @Mock
    private IntegrationWebhookService integrationWebhookService;

    private IntegrationWebhookDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new IntegrationWebhookDeliveryService(deliveryRepo, integrationWebhookService, new ObjectMapper());
    }

    @Test
    void page_mapsRecords() {
        LlmIntegrationWebhookDeliveryDO row = new LlmIntegrationWebhookDeliveryDO();
        row.setId(1L);
        row.setTaskCode("aiChat");
        row.setStatus("SENT");
        row.setAttemptCount(1);
        row.setMaxAttempts(2);
        row.setDeadLetter(false);
        Page<LlmIntegrationWebhookDeliveryDO> raw = new Page<>(1, 20, 1);
        raw.setRecords(java.util.List.of(row));
        when(deliveryRepo.page(null, 1, 20)).thenReturn(raw);

        Page<?> page = service.page(null, 1, 20);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void retry_rejectsSentDelivery() {
        LlmIntegrationWebhookDeliveryDO row = new LlmIntegrationWebhookDeliveryDO();
        row.setId(2L);
        row.setStatus("SENT");
        row.setDeadLetter(false);
        when(deliveryRepo.findById(2L)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.retry(2L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("DELIVERY_NOT_RETRYABLE");
    }

    @Test
    void retry_invokesRedeliverForDeadLetter() {
        LlmIntegrationWebhookDeliveryDO row = new LlmIntegrationWebhookDeliveryDO();
        row.setId(3L);
        row.setStatus("FAILED");
        row.setDeadLetter(true);
        row.setDetailJson("{\"event\":\"PROFILE_PUBLISH_SUCCESS\",\"taskCode\":\"aiChat\"}");
        when(deliveryRepo.findById(3L)).thenReturn(Optional.of(row), Optional.of(row));

        service.retry(3L);

        verify(integrationWebhookService).redeliver(org.mockito.ArgumentMatchers.eq(row), org.mockito.ArgumentMatchers.anyMap());
    }
}
