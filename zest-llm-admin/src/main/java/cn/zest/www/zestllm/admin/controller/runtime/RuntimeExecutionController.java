package cn.zest.www.zestllm.admin.controller.runtime;

import cn.zest.www.zestllm.admin.model.vo.ExecutionVO;
import cn.zest.www.zestllm.admin.service.ExecutionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/executions")
@RequiredArgsConstructor
public class RuntimeExecutionController {

    private final ExecutionQueryService executionQueryService;

    @GetMapping("/{traceId}")
    public ExecutionVO getExecution(@PathVariable String traceId) {
        return executionQueryService.getByTraceId(traceId);
    }
}
