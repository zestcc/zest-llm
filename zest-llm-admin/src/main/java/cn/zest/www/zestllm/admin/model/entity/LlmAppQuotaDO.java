package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_app_quota")
public class LlmAppQuotaDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long appId;
    private Long dailyTokenLimit;
    private Integer qpsLimit;
    private BigDecimal dailyCostLimit;
    private String alertWebhookUrl;
    private Integer alertThresholdPct;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
