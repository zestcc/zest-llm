package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmIntegrationWebhookDeliveryDO;
import cn.zest.www.zestllm.admin.model.vo.IntegrationWebhookDeliveryVO;
import cn.zest.www.zestllm.admin.repo.LlmIntegrationWebhookDeliveryRepo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntegrationWebhookDeliveryService {

    private final LlmIntegrationWebhookDeliveryRepo deliveryRepo;
    private final IntegrationWebhookService integrationWebhookService;
    private final ObjectMapper objectMapper;

    public Page<IntegrationWebhookDeliveryVO> page(String taskCode, int pageNum, int pageSize) {
        Page<LlmIntegrationWebhookDeliveryDO> raw = deliveryRepo.page(taskCode, pageNum, pageSize);
        Page<IntegrationWebhookDeliveryVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public IntegrationWebhookDeliveryVO retry(Long id) {
        LlmIntegrationWebhookDeliveryDO row = deliveryRepo.findById(id)
                .orElseThrow(() -> new BusinessException("DELIVERY_NOT_FOUND", "Webhook 投递记录不存在"));
        if (!Boolean.TRUE.equals(row.getDeadLetter()) && "SENT".equals(row.getStatus())) {
            throw new BusinessException("DELIVERY_NOT_RETRYABLE", "仅失败或死信记录可重试");
        }
        Map<String, Object> payload = parsePayload(row);
        integrationWebhookService.redeliver(row, payload);
        return deliveryRepo.findById(id).map(this::toVO)
                .orElseThrow(() -> new BusinessException("DELIVERY_NOT_FOUND", "Webhook 投递记录不存在"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(LlmIntegrationWebhookDeliveryDO row) {
        if (row.getDetailJson() == null || row.getDetailJson().isBlank()) {
            throw new BusinessException("DELIVERY_PAYLOAD_MISSING", "缺少可重试的 payload");
        }
        try {
            return objectMapper.readValue(row.getDetailJson(), Map.class);
        } catch (Exception ex) {
            throw new BusinessException("DELIVERY_PAYLOAD_INVALID", "payload 解析失败");
        }
    }

    private IntegrationWebhookDeliveryVO toVO(LlmIntegrationWebhookDeliveryDO row) {
        return IntegrationWebhookDeliveryVO.builder()
                .id(row.getId())
                .eventType(row.getEventType())
                .taskCode(row.getTaskCode())
                .profileVersion(row.getProfileVersion())
                .webhookUrl(row.getWebhookUrl())
                .payloadHash(row.getPayloadHash())
                .status(row.getStatus())
                .attemptCount(row.getAttemptCount() != null ? row.getAttemptCount() : 0)
                .maxAttempts(row.getMaxAttempts() != null ? row.getMaxAttempts() : 1)
                .lastError(row.getLastError())
                .deadLetter(Boolean.TRUE.equals(row.getDeadLetter()))
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
