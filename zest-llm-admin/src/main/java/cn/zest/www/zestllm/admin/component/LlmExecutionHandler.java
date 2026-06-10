package cn.zest.www.zestllm.admin.component;

import cn.zest.www.zestllm.admin.model.vo.ExecutionVO;
import cn.zest.www.zestllm.admin.service.ExecutionQueryService;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import com.zestflow.executor.annotation.ZestComponent;
import com.zestflow.executor.annotation.ZestExecute;
import com.zestflow.executor.annotation.ZestParam;
import com.zestflow.executor.annotation.ZestResult;
import lombok.RequiredArgsConstructor;

/**
 * Execution 查询元件 — 供 ZestFlow 编排审计/追溯节点。
 */
@ZestComponent("llmExecutionHandler")
@RequiredArgsConstructor
public class LlmExecutionHandler {

    private final ExecutionQueryService executionQueryService;

    /**
     * 按 traceId 查询执行记录。
     *
     * @param traceId 链路 ID
     * @return 执行详情
     */
    @ZestExecute(value = "getExecution", name = "查询 Execution")
    public ExecutionVO getExecution(@ZestParam("traceId") String traceId) {
        return executionQueryService.getByTraceId(traceId);
    }

    /**
     * 链式第二跳：从上一步 invoke 结果取 traceId 查询 Execution。
     */
    @ZestExecute(value = "getExecutionFromInvoke", name = "查询上一步 Execution")
    public ExecutionVO getExecutionFromInvoke(@ZestResult InvokeResponse invokeResponse) {
        if (invokeResponse == null || invokeResponse.getTraceId() == null) {
            return null;
        }
        return executionQueryService.getByTraceId(invokeResponse.getTraceId());
    }
}
