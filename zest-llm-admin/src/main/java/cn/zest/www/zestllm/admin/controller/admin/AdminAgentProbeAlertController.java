package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.AgentProbeAlertVO;
import cn.zest.www.zestllm.admin.service.AgentProbeAlertService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/agent-probe-alerts")
@RequiredArgsConstructor
public class AdminAgentProbeAlertController {

    private final AgentProbeAlertService agentProbeAlertService;

    @GetMapping
    public Result<List<AgentProbeAlertVO>> list(@RequestParam(required = false) String taskCode,
                                                @RequestParam(defaultValue = "20") int limit) {
        return Result.success(agentProbeAlertService.listRecent(taskCode, limit));
    }
}
