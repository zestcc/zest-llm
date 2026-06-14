package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IntegrationWebhookDeliveryVO {
    private Long id;
    private String eventType;
    private String taskCode;
    private String profileVersion;
    private String webhookUrl;
    private String payloadHash;
    private String status;
    private int attemptCount;
    private int maxAttempts;
    private String lastError;
    private boolean deadLetter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
