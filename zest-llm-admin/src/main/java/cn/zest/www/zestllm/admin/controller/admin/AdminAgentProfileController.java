package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.request.CreateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.ImportAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.PublishAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.RollbackAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeBatchResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfilePublishResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileVO;
import cn.zest.www.zestllm.admin.model.vo.PublishPreviewVO;
import cn.zest.www.zestllm.admin.model.vo.VersionDiffVO;
import cn.zest.www.zestllm.admin.service.AgentProfilePublishPreviewService;
import cn.zest.www.zestllm.admin.service.AgentProfileProbeRecordService;
import cn.zest.www.zestllm.admin.service.AgentProfileDiffService;
import cn.zest.www.zestllm.admin.service.AgentProfileManageService;
import cn.zest.www.zestllm.admin.service.AgentProfileProbeService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/agent-profiles")
@RequiredArgsConstructor
public class AdminAgentProfileController {

    private final AgentProfileManageService agentProfileManageService;
    private final AgentProfileDiffService agentProfileDiffService;
    private final AgentProfileProbeService agentProfileProbeService;
    private final AgentProfileProbeRecordService agentProfileProbeRecordService;
    private final AgentProfilePublishPreviewService agentProfilePublishPreviewService;

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

    @GetMapping("/{taskCode}/versions/{version}/publish-preview")
    public Result<PublishPreviewVO> publishPreview(@PathVariable String taskCode, @PathVariable String version) {
        return Result.success(agentProfilePublishPreviewService.preview(taskCode, version));
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

    @PostMapping("/probe-all")
    public Result<AgentProfileProbeBatchResultVO> probeAll(@RequestBody(required = false) AgentProfileProbeRequest request) {
        boolean smokeTest = request != null && request.isSmokeTest();
        int count = agentProfileProbeService.probeAllPublished(smokeTest, AgentProfileProbeRecordService.SOURCE_MANUAL);
        return Result.success(AgentProfileProbeBatchResultVO.builder().probedCount(count).build());
    }

    /** @deprecated prefer {@code POST /api/admin/agent-profile-probes/run-all} */
    @PostMapping("/{taskCode}/probe")
    public Result<AgentProfileProbeResultVO> probePublished(@PathVariable String taskCode,
                                                            @RequestBody(required = false) AgentProfileProbeRequest request) {
        return Result.success(agentProfileProbeService.probePublished(taskCode, request));
    }

    @PostMapping("/{taskCode}/versions/{version}/probe")
    public Result<AgentProfileProbeResultVO> probeVersion(@PathVariable String taskCode,
                                                          @PathVariable String version,
                                                          @RequestBody(required = false) AgentProfileProbeRequest request) {
        return Result.success(agentProfileProbeService.probeVersion(taskCode, version, request));
    }

    @GetMapping("/{taskCode}/probe/latest")
    public Result<AgentProfileProbeResultVO> probeLatest(@PathVariable String taskCode) {
        return Result.success(agentProfileProbeRecordService.latest(taskCode)
                .orElse(null));
    }

    /** @deprecated prefer {@code GET /api/admin/agent-profile-probes/{taskCode}/history} */
    @GetMapping("/{taskCode}/probe/history")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AgentProfileProbeResultVO>> probeHistory(
            @PathVariable String taskCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String profileVersion) {
        return Result.success(agentProfileProbeRecordService.history(taskCode, page, size, profileVersion));
    }
}
