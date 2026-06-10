package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreatePromptVersionRequest;
import cn.zest.www.zestllm.admin.model.request.PublishPromptRequest;
import cn.zest.www.zestllm.admin.model.request.RollbackPromptRequest;
import cn.zest.www.zestllm.admin.model.vo.PromptPublishResultVO;
import cn.zest.www.zestllm.admin.model.vo.PromptVersionVO;
import cn.zest.www.zestllm.admin.model.vo.VersionDiffVO;
import cn.zest.www.zestllm.admin.service.AdminQueryService;
import cn.zest.www.zestllm.admin.service.PromptDiffService;
import cn.zest.www.zestllm.admin.service.PromptPublishService;
import cn.zest.www.zestllm.admin.service.PromptVersionService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptController {

    private final AdminQueryService adminQueryService;
    private final PromptPublishService promptPublishService;
    private final PromptVersionService promptVersionService;
    private final PromptDiffService promptDiffService;

    @GetMapping("/{code}/versions")
    public Result<List<PromptVersionVO>> listVersions(@PathVariable String code) {
        return Result.success(adminQueryService.listPromptVersions(code));
    }

    @GetMapping("/{code}/diff")
    public Result<VersionDiffVO> diff(@PathVariable String code,
                                        @org.springframework.web.bind.annotation.RequestParam String from,
                                        @org.springframework.web.bind.annotation.RequestParam String to) {
        return Result.success(promptDiffService.diff(code, from, to));
    }

    @PostMapping("/{code}/versions")
    public Result<PromptVersionVO> createVersion(@PathVariable String code,
                                                 @Valid @RequestBody CreatePromptVersionRequest request) {
        return Result.success(promptVersionService.createVersion(code, request));
    }

    @PostMapping("/{code}/publish")
    public Result<PromptPublishResultVO> publish(@PathVariable String code,
                                                 @Valid @RequestBody PublishPromptRequest request) {
        return Result.success(promptPublishService.publish(code, request.getVersion(), request.getOperator()));
    }

    @PostMapping("/{code}/rollback")
    public Result<PromptPublishResultVO> rollback(@PathVariable String code,
                                                  @Valid @RequestBody RollbackPromptRequest request) {
        return Result.success(promptVersionService.rollback(code, request.getVersion()));
    }
}
