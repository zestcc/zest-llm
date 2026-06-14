package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_integration_webhook_delivery")
public class LlmIntegrationWebhookDeliveryDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String eventType;
    private String taskCode;
    private String profileVersion;
    private String webhookUrl;
    private String payloadHash;
    private String status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private String lastError;
    private Boolean deadLetter;
    private String detailJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
