package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.ImportAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.PublishAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.RollbackAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfilePublishResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileVO;
import cn.zest.www.zestllm.admin.model.vo.VersionDiffVO;
import cn.zest.www.zestllm.admin.service.AgentProfileDiffService;
import cn.zest.www.zestllm.admin.service.AgentProfileManageService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/agent-profiles")
@RequiredArgsConstructor
public class AdminAgentProfileController {

    private final AgentProfileManageService agentProfileManageService;
    private final AgentProfileDiffService agentProfileDiffService;

    @GetMapping("/{taskCode}/versions")
    public Result<List<AgentProfileVO>> listVersions(@PathVariable String taskCode) {
        return Result.success(agentProfileManageService.listVersions(taskCode));
    }

    @GetMapping("/{taskCode}/published")
    public Result<AgentProfileVO> published(@PathVariable String taskCode) {
        return Result.success(agentProfileManageService.getPublished(taskCode));
    }

    @GetMapping("/{taskCode}/diff")
    public Result<VersionDiffVO> diff(@PathVariable String taskCode,
                                      @org.springframework.web.bind.annotation.RequestParam String from,
                                      @org.springframework.web.bind.annotation.RequestParam String to) {
        return Result.success(agentProfileDiffService.diff(taskCode, from, to));
    }

    @PostMapping("/{taskCode}/versions")
    public Result<AgentProfileVO> createVersion(@PathVariable String taskCode,
                                                @Valid @RequestBody CreateAgentProfileRequest request) {
        return Result.success(agentProfileManageService.createVersion(taskCode, request));
    }

    @PutMapping("/{taskCode}/versions/{version}")
    public Result<AgentProfileVO> updateVersion(@PathVariable String taskCode,
                                                @PathVariable String version,
                                                @Valid @RequestBody UpdateAgentProfileRequest request) {
        return Result.success(agentProfileManageService.updateVersion(taskCode, version, request));
    }

    @PostMapping("/{taskCode}/publish")
    public Result<AgentProfilePublishResultVO> publish(@PathVariable String taskCode,
                                                       @Valid @RequestBody PublishAgentProfileRequest request) {
        return Result.success(agentProfileManageService.publish(taskCode, request.getVersion(), request.getOperator()));
    }

    @PostMapping("/{taskCode}/rollback")
    public Result<AgentProfilePublishResultVO> rollback(@PathVariable String taskCode,
                                                        @Valid @RequestBody RollbackAgentProfileRequest request) {
        return Result.success(agentProfileManageService.rollback(taskCode, request.getVersion()));
    }

    @PostMapping("/import")
    public Result<AgentProfileVO> importProfile(@Valid @RequestBody ImportAgentProfileRequest request) {
        return Result.success(agentProfileManageService.importProfile(request));
    }

    @GetMapping("/{taskCode}/versions/{version}/export")
    public Result<Map<String, String>> exportProfile(@PathVariable String taskCode, @PathVariable String version) {
        return Result.success(Map.of("profileJson", agentProfileManageService.exportProfile(taskCode, version)));
    }

    @PostMapping("/{taskCode}/activate-provider")
    public Result<Void> activateProvider(@PathVariable String taskCode,
                                         @RequestBody Map<String, String> body) {
        agentProfileManageService.activateProvider(taskCode, body.get("providerRef"));
        return Result.success(null);
    }
}
