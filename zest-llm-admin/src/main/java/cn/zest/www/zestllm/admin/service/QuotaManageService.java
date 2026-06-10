package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppQuotaDO;
import cn.zest.www.zestllm.admin.model.request.UpdateQuotaRequest;
import cn.zest.www.zestllm.admin.model.vo.QuotaVO;
import cn.zest.www.zestllm.admin.repo.LlmAppQuotaRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuotaManageService {

    private final LlmAppRepo appRepo;
    private final LlmAppQuotaRepo quotaRepo;
    private final AuditService auditService;

    public QuotaVO get(String appKey) {
        LlmAppDO app = requireApp(appKey);
        LlmAppQuotaDO quota = quotaRepo.findByAppId(app.getId())
                .orElseThrow(() -> new BusinessException("QUOTA_NOT_FOUND", "配额未配置: " + appKey));
        return toVO(appKey, quota);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuotaVO update(String appKey, UpdateQuotaRequest request) {
        LlmAppDO app = requireApp(appKey);
        LlmAppQuotaDO quota = quotaRepo.findByAppId(app.getId()).orElseGet(() -> {
            LlmAppQuotaDO created = new LlmAppQuotaDO();
            created.setAppId(app.getId());
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        if (request.getDailyTokenLimit() != null) {
            quota.setDailyTokenLimit(request.getDailyTokenLimit());
        }
        if (request.getQpsLimit() != null) {
            quota.setQpsLimit(request.getQpsLimit());
        }
        if (request.getDailyCostLimit() != null) {
            quota.setDailyCostLimit(request.getDailyCostLimit());
        }
        if (request.getAlertWebhookUrl() != null) {
            quota.setAlertWebhookUrl(request.getAlertWebhookUrl());
        }
        if (request.getAlertThresholdPct() != null) {
            quota.setAlertThresholdPct(request.getAlertThresholdPct());
        }
        quota.setUpdatedAt(LocalDateTime.now());
        if (quota.getId() == null) {
            quotaRepo.insert(quota);
        } else {
            quotaRepo.update(quota);
        }
        auditService.log("UPDATE", "QUOTA", appKey, Map.of(
                "dailyTokenLimit", quota.getDailyTokenLimit(),
                "qpsLimit", quota.getQpsLimit(),
                "dailyCostLimit", quota.getDailyCostLimit()));
        return toVO(appKey, quota);
    }

    private LlmAppDO requireApp(String appKey) {
        return appRepo.findByAppKey(appKey)
                .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在: " + appKey));
    }

    private QuotaVO toVO(String appKey, LlmAppQuotaDO quota) {
        return QuotaVO.builder()
                .appKey(appKey)
                .dailyTokenLimit(quota.getDailyTokenLimit())
                .qpsLimit(quota.getQpsLimit())
                .dailyCostLimit(quota.getDailyCostLimit())
                .alertWebhookUrl(quota.getAlertWebhookUrl())
                .alertThresholdPct(quota.getAlertThresholdPct())
                .updatedAt(quota.getUpdatedAt())
                .build();
    }
}
