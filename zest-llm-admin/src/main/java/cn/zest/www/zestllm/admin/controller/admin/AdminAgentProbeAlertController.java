package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.AgentProbeAlertVO;
import cn.zest.www.zestllm.admin.service.AgentProbeAlertService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin/agent-probe-alerts")
@RequiredArgsConstructor
public class AdminAgentProbeAlertController {

    private final AgentProbeAlertService agentProbeAlertService;

    @GetMapping
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AgentProbeAlertVO>> list(
            @RequestParam(required = false) String taskCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(agentProbeAlertService.page(taskCode, page, size));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AgentProbeAlertVO> resend(@PathVariable Long id) {
        return Result.success(agentProbeAlertService.resend(id));
    }
}
