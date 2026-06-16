package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.ExecutionVO;
import cn.zest.www.zestllm.admin.service.ExecutionQueryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/executions")
@RequiredArgsConstructor
public class AdminExecutionController {

    private final ExecutionQueryService executionQueryService;

    @GetMapping
    public Result<Page<ExecutionVO>> listExecutions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String appKey) {
        return Result.success(executionQueryService.page(page, size, taskCode, status, appKey));
    }

    @GetMapping("/{traceId}")
    public Result<ExecutionVO> getExecution(@PathVariable String traceId) {
        return Result.success(executionQueryService.getByTraceId(traceId));
    }
}
