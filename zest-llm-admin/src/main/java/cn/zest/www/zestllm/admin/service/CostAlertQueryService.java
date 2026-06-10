package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.mapper.LlmCostAlertMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmCostAlertDO;
import cn.zest.www.zestllm.admin.model.vo.CostAlertVO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CostAlertQueryService {

    private final LlmCostAlertMapper costAlertMapper;
    private final LlmAppRepo appRepo;

    public List<CostAlertVO> listRecent(String appKey) {
        LambdaQueryWrapper<LlmCostAlertDO> query = new LambdaQueryWrapper<LlmCostAlertDO>()
                .orderByDesc(LlmCostAlertDO::getCreatedAt)
                .last("LIMIT 20");
        if (appKey != null && !appKey.isBlank()) {
            LlmAppDO app = appRepo.findByAppKey(appKey).orElse(null);
            if (app == null) {
                return List.of();
            }
            query.eq(LlmCostAlertDO::getAppId, app.getId());
        }
        List<LlmCostAlertDO> rows = costAlertMapper.selectList(query);
        return rows.stream()
                .map(row -> {
                    String key = appRepo.findById(row.getAppId()).map(LlmAppDO::getAppKey).orElse(null);
                    return CostAlertVO.builder()
                            .appKey(key)
                            .alertDate(row.getAlertDate())
                            .dailyCost(row.getDailyCost())
                            .costLimit(row.getCostLimit())
                            .thresholdPct(row.getThresholdPct())
                            .status(row.getStatus())
                            .createdAt(row.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
