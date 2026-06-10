package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.ExecutionArchiveStatsVO;
import cn.zest.www.zestllm.admin.service.ExecutionArchiveAdminService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/executions/archive")
@RequiredArgsConstructor
public class AdminExecutionArchiveController {

    private final ExecutionArchiveAdminService archiveAdminService;

    @GetMapping("/stats")
    public Result<ExecutionArchiveStatsVO> stats() {
        return Result.success(archiveAdminService.stats());
    }

    @PostMapping("/run")
    public Result<ExecutionArchiveStatsVO> runNow() {
        return Result.success(archiveAdminService.runNow());
    }
}
