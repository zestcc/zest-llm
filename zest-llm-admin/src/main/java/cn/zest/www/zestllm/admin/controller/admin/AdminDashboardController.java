package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.AgentHealthDashboardVO;
import cn.zest.www.zestllm.admin.model.vo.DailyCostVO;
import cn.zest.www.zestllm.admin.model.vo.DashboardStatsVO;
import cn.zest.www.zestllm.admin.service.DashboardService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.stats());
    }

    @GetMapping("/cost")
    public Result<List<DailyCostVO>> cost(@RequestParam(defaultValue = "7") int days) {
        return Result.success(dashboardService.costLastDays(days));
    }

    @GetMapping("/agent-health")
    public Result<AgentHealthDashboardVO> agentHealth() {
        return Result.success(dashboardService.agentHealth());
    }
}
