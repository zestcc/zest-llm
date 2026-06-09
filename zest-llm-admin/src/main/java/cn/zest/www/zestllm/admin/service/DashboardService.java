package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.DailyCostVO;
import cn.zest.www.zestllm.admin.model.vo.DashboardStatsVO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LlmAppRepo appRepo;
    private final LlmExecutionRepo executionRepo;

    public DashboardStatsVO stats() {
        return DashboardStatsVO.builder()
                .apps(appRepo.countActive())
                .executions(executionRepo.countAll())
                .success(executionRepo.countSuccess())
                .failed(executionRepo.countFailed())
                .totalCost(executionRepo.sumTotalCost())
                .todayExecutions(executionRepo.countToday())
                .build();
    }

    public List<DailyCostVO> costLastDays(int days) {
        int window = Math.max(1, Math.min(days, 90));
        LocalDate start = LocalDate.now().minusDays(window - 1L);
        return executionRepo.dailyCostSince(start.atStartOfDay());
    }
}
