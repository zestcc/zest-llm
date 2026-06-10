package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("llm_cost_alert")
public class LlmCostAlertDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long appId;
    private LocalDate alertDate;
    private BigDecimal dailyCost;
    private BigDecimal costLimit;
    private Integer thresholdPct;
    private String webhookUrl;
    private String status;
    private String detailJson;
    private LocalDateTime createdAt;
}
