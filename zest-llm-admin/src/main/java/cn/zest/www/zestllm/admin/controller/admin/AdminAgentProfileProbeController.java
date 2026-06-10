package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeBatchResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeCompareVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeTrendPointVO;
import cn.zest.www.zestllm.admin.service.AgentProfileProbeRecordService;
import cn.zest.www.zestllm.admin.service.AgentProfileProbeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/agent-profile-probes")
@RequiredArgsConstructor
public class AdminAgentProfileProbeController {

    private final AgentProfileProbeService agentProfileProbeService;
    private final AgentProfileProbeRecordService agentProfileProbeRecordService;
    private final ObjectMapper objectMapper;

    @PostMapping("/run-all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AgentProfileProbeBatchResultVO> runAll(@RequestBody(required = false) AgentProfileProbeRequest request) {
        boolean smokeTest = request != null && request.isSmokeTest();
        int count = agentProfileProbeService.probeAllPublished(smokeTest, AgentProfileProbeRecordService.SOURCE_MANUAL);
        return Result.success(AgentProfileProbeBatchResultVO.builder().probedCount(count).build());
    }

    @PostMapping("/{taskCode}/run")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AgentProfileProbeResultVO> runPublished(@PathVariable String taskCode,
                                                           @RequestBody(required = false) AgentProfileProbeRequest request) {
        return Result.success(agentProfileProbeService.probePublished(taskCode, request));
    }

    @PostMapping("/{taskCode}/run-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AgentProfileProbeResultVO> runFailed(@PathVariable String taskCode,
                                                       @RequestBody(required = false) AgentProfileProbeRequest request) {
        return Result.success(agentProfileProbeService.probeRetryFailed(taskCode, request));
    }

    @PostMapping("/{taskCode}/versions/{version}/run")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AgentProfileProbeResultVO> runVersion(@PathVariable String taskCode,
                                                        @PathVariable String version,
                                                        @RequestBody(required = false) AgentProfileProbeRequest request) {
        return Result.success(agentProfileProbeService.probeVersion(taskCode, version, request));
    }

    @GetMapping("/{taskCode}/latest")
    public Result<AgentProfileProbeResultVO> latest(@PathVariable String taskCode) {
        return Result.success(agentProfileProbeRecordService.latest(taskCode).orElse(null));
    }

    @GetMapping("/{taskCode}/history")
    public Result<Page<AgentProfileProbeResultVO>> history(@PathVariable String taskCode,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) String profileVersion) {
        return Result.success(agentProfileProbeRecordService.history(taskCode, page, size, profileVersion));
    }

    @GetMapping("/{taskCode}/history/trend")
    public Result<List<AgentProfileProbeTrendPointVO>> trend(@PathVariable String taskCode,
                                                             @RequestParam(defaultValue = "7") int days,
                                                             @RequestParam(required = false) String profileVersion) {
        return Result.success(agentProfileProbeRecordService.trend(taskCode, days, profileVersion));
    }

    @GetMapping("/{taskCode}/compare")
    public Result<AgentProfileProbeCompareVO> compare(@PathVariable String taskCode,
                                                      @RequestParam String fromVersion,
                                                      @RequestParam String toVersion) {
        return Result.success(agentProfileProbeRecordService.compareVersions(taskCode, fromVersion, toVersion));
    }

    @GetMapping("/{taskCode}/history/export")
    public ResponseEntity<byte[]> export(@PathVariable String taskCode,
                                         @RequestParam(defaultValue = "json") String format,
                                         @RequestParam(required = false) String profileVersion,
                                         @RequestParam(defaultValue = "500") int limit) {
        if ("csv".equalsIgnoreCase(format)) {
            String csv = agentProfileProbeRecordService.exportHistoryCsv(taskCode, profileVersion, limit);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"probe-history-" + taskCode + ".csv\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv.getBytes(StandardCharsets.UTF_8));
        }
        List<AgentProfileProbeResultVO> rows = agentProfileProbeRecordService.exportHistory(taskCode, profileVersion, limit);
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"probe-history-" + taskCode + ".json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsBytes(rows));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to export probe history", ex);
        }
    }
}
