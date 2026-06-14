package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.config.IntegrationWebhookProperties;
import cn.zest.www.zestllm.admin.model.entity.LlmIntegrationWebhookDeliveryDO;
import cn.zest.www.zestllm.admin.repo.LlmIntegrationWebhookDeliveryRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationWebhookServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private LlmIntegrationWebhookDeliveryRepo deliveryRepo;

    private IntegrationWebhookProperties properties;
    private IntegrationWebhookService webhookService;

    @BeforeEach
    void setUp() {
        properties = new IntegrationWebhookProperties();
        properties.setWebhookUrl("http://localhost:9999/hook");
        properties.setWebhookMaxRetries(1);
        properties.setWebhookRetryDelayMs(10L);
        webhookService = new IntegrationWebhookService(properties, restClientBuilder, new ObjectMapper(), deliveryRepo);
    }

    @Test
    void notifyPublishResult_skipsWhenWebhookUrlEmpty() {
        properties.setWebhookUrl("");
        webhookService.notifyPublishResult("aiChat", "v1", true, "ok", "admin");
        verify(deliveryRepo, never()).insert(any());
    }

    @Test
    void notifyPublishResult_recordsSentDelivery() {
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);
        doAnswer(inv -> {
            LlmIntegrationWebhookDeliveryDO row = inv.getArgument(0);
            row.setId(100L);
            return null;
        }).when(deliveryRepo).insert(any());

        webhookService.notifyPublishResult("aiChat", "v1", true, "published", "admin");

        verify(deliveryRepo).updateById(any());
    }

    @Test
    void notifyPublishResult_marksDeadLetterAfterMaxRetries() {
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("connection refused"));
        doAnswer(inv -> {
            LlmIntegrationWebhookDeliveryDO row = inv.getArgument(0);
            row.setId(101L);
            return null;
        }).when(deliveryRepo).insert(any());

        webhookService.notifyPublishResult("aiChat", "v1", false, "failed", "admin");

        verify(deliveryRepo, org.mockito.Mockito.atLeastOnce()).updateById(org.mockito.ArgumentMatchers.argThat(row ->
                Boolean.TRUE.equals(row.getDeadLetter()) && "FAILED".equals(row.getStatus())));
    }
}
