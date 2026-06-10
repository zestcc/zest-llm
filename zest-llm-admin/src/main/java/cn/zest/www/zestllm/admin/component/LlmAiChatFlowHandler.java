package cn.zest.www.zestllm.admin.component;

import cn.zest.www.zestllm.admin.model.dto.InvokeCommand;
import cn.zest.www.zestllm.admin.service.ExecutionQueryService;
import cn.zest.www.zestllm.admin.service.LlmInvokeService;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import com.zestflow.executor.annotation.ZestComponent;
import com.zestflow.executor.annotation.ZestExecute;
import com.zestflow.executor.annotation.ZestParam;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI Chat 全流程元件（E2E 单节点快捷路径，内部委托 invoke + execution 查询）。
 */
@ZestComponent("llmAiChatFlowHandler")
@RequiredArgsConstructor
public class LlmAiChatFlowHandler {

    private static final String DEFAULT_APP_KEY = "order-service";
    private static final String DEFAULT_CODE = "aiChat";
    private static final String DEFAULT_BEARER = "demo-token-123";

    private final LlmInvokeService llmInvokeService;
    private final ExecutionQueryService executionQueryService;

    @ZestExecute(value = "runAiChatFlow", name = "AI Chat 全流程")
    public Map<String, Object> runAiChatFlow(@ZestParam("question") String question,
                                             @ZestParam("bearerToken") String bearerToken) {
        InvokeResponse response = llmInvokeService.invoke(buildCommand(question, bearerToken));
        var execution = executionQueryService.getByTraceId(response.getTraceId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traceId", response.getTraceId());
        result.put("status", response.getStatus());
        result.put("output", response.getOutput());
        result.put("executionStatus", execution != null ? execution.getStatus() : null);
        return result;
    }

    private InvokeCommand buildCommand(String question, String bearerToken) {
        InvokeRequest request = new InvokeRequest();
        request.setAppKey(DEFAULT_APP_KEY);
        request.setCode(DEFAULT_CODE);
        request.setInputs(Map.of("question", question != null ? question : "hello"));
        InvokeCommand command = new InvokeCommand();
        command.setBearerToken(bearerToken != null && !bearerToken.isBlank() ? bearerToken : DEFAULT_BEARER);
        command.setRequest(request);
        return command;
    }
}
