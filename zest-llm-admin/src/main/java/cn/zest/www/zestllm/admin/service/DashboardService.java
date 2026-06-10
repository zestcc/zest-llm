package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.AgentHealthDashboardVO;
import cn.zest.www.zestllm.admin.model.vo.DailyCostVO;
import cn.zest.www.zestllm.admin.model.vo.DashboardStatsVO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.admin.service.AgentProfileProbeRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LlmAppRepo appRepo;
    private final LlmExecutionRepo executionRepo;
    private final AgentProfileProbeRecordService agentProfileProbeRecordService;

    public DashboardStatsVO stats() {
        AgentHealthDashboardVO agentHealth = agentProfileProbeRecordService.dashboardHealth();
        return DashboardStatsVO.builder()
                .apps(appRepo.countActive())
                .executions(executionRepo.countAll())
                .success(executionRepo.countSuccess())
                .failed(executionRepo.countFailed())
                .totalCost(executionRepo.sumTotalCost())
                .todayExecutions(executionRepo.countToday())
                .agentsMonitored(agentHealth.getMonitored())
                .agentsReady(agentHealth.getReady())
                .agentsDegraded(agentHealth.getDegraded())
                .agentsUnavailable(agentHealth.getUnavailable())
                .agentsUnknown(agentHealth.getUnknown())
                .build();
    }

    public AgentHealthDashboardVO agentHealth() {
        return agentProfileProbeRecordService.dashboardHealth();
    }

    public List<DailyCostVO> costLastDays(int days) {
        int window = Math.max(1, Math.min(days, 90));
        LocalDate start = LocalDate.now().minusDays(window - 1L);
        return executionRepo.dailyCostSince(start.atStartOfDay());
    }
}
