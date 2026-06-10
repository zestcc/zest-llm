package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.mapper.LlmCostAlertMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmCostAlertDO;
import cn.zest.www.zestllm.admin.model.vo.CostAlertVO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CostAlertQueryService {

    private final LlmCostAlertMapper costAlertMapper;
    private final LlmAppRepo appRepo;
    private final ObjectMapper objectMapper;

    public Page<CostAlertVO> page(String appKey, int pageNum, int pageSize) {
        LambdaQueryWrapper<LlmCostAlertDO> query = new LambdaQueryWrapper<LlmCostAlertDO>()
                .orderByDesc(LlmCostAlertDO::getCreatedAt);
        if (appKey != null && !appKey.isBlank()) {
            LlmAppDO app = appRepo.findByAppKey(appKey).orElse(null);
            if (app == null) {
                return emptyPage(pageNum, pageSize);
            }
            query.eq(LlmCostAlertDO::getAppId, app.getId());
        }
        Page<LlmCostAlertDO> pager = new Page<>(pageNum, pageSize);
        costAlertMapper.selectPage(pager, query);
        Page<CostAlertVO> result = new Page<>(pager.getCurrent(), pager.getSize(), pager.getTotal());
        result.setRecords(pager.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    /** @deprecated use {@link #page(String, int, int)} */
    public List<CostAlertVO> listRecent(String appKey) {
        return page(appKey, 1, 20).getRecords();
    }

    private CostAlertVO toVO(LlmCostAlertDO row) {
        String key = appRepo.findById(row.getAppId()).map(LlmAppDO::getAppKey).orElse(null);
        return CostAlertVO.builder()
                .appKey(key)
                .alertDate(row.getAlertDate())
                .dailyCost(row.getDailyCost())
                .costLimit(row.getCostLimit())
                .thresholdPct(row.getThresholdPct())
                .status(row.getStatus())
                .message(extractMessage(row.getDetailJson()))
                .createdAt(row.getCreatedAt())
                .build();
    }

    private String extractMessage(String detailJson) {
        if (!StringUtils.hasText(detailJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(detailJson).path("message").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private Page<CostAlertVO> emptyPage(int pageNum, int pageSize) {
        Page<CostAlertVO> empty = new Page<>(pageNum, pageSize, 0);
        empty.setRecords(List.of());
        return empty;
    }
}
