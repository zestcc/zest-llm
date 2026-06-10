package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.mapper.LlmCostAlertMapper;
import cn.zest.www.zestllm.admin.mapper.LlmExecutionMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppQuotaDO;
import cn.zest.www.zestllm.admin.model.entity.LlmCostAlertDO;
import cn.zest.www.zestllm.admin.repo.LlmAppQuotaRepo;
import cn.zest.www.zestllm.spi.alert.AlertWebhookAdapter;
import cn.zest.www.zestllm.spi.alert.CostAlertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostAlertService {

    private final LlmAppQuotaRepo quotaRepo;
    private final LlmExecutionMapper executionMapper;
    private final LlmCostAlertMapper costAlertMapper;
    private final AlertWebhookAdapter alertWebhookAdapter;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void checkAfterInvoke(LlmAppDO app) {
        LlmAppQuotaDO quota = quotaRepo.findByAppId(app.getId()).orElse(null);
        if (quota == null || quota.getDailyCostLimit() == null || quota.getDailyCostLimit().signum() <= 0) {
            return;
        }
        if (!StringUtils.hasText(quota.getAlertWebhookUrl())) {
            return;
        }
        BigDecimal dailyCost = executionMapper.sumTodayCostByAppId(app.getId());
        if (dailyCost == null) {
            dailyCost = BigDecimal.ZERO;
        }
        int thresholdPct = quota.getAlertThresholdPct() != null ? quota.getAlertThresholdPct() : 80;
        BigDecimal threshold = quota.getDailyCostLimit()
                .multiply(BigDecimal.valueOf(thresholdPct))
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        if (dailyCost.compareTo(threshold) < 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (costAlertMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LlmCostAlertDO>()
                .eq(LlmCostAlertDO::getAppId, app.getId())
                .eq(LlmCostAlertDO::getAlertDate, today)) > 0) {
            return;
        }

        String message = String.format("App %s daily cost %.6f reached %d%% of limit %.4f",
                app.getAppKey(), dailyCost, thresholdPct, quota.getDailyCostLimit());
        CostAlertEvent event = CostAlertEvent.builder()
                .appKey(app.getAppKey())
                .alertDate(today)
                .dailyCost(dailyCost)
                .costLimit(quota.getDailyCostLimit())
                .thresholdPct(thresholdPct)
                .message(message)
                .build();

        LlmCostAlertDO row = new LlmCostAlertDO();
        row.setAppId(app.getId());
        row.setAlertDate(today);
        row.setDailyCost(dailyCost);
        row.setCostLimit(quota.getDailyCostLimit());
        row.setThresholdPct(thresholdPct);
        row.setWebhookUrl(quota.getAlertWebhookUrl());
        row.setStatus("SENT");
        row.setDetailJson(toJson(Map.of("message", message)));
        row.setCreatedAt(LocalDateTime.now());
        costAlertMapper.insert(row);

        alertWebhookAdapter.send(quota.getAlertWebhookUrl(), event);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
