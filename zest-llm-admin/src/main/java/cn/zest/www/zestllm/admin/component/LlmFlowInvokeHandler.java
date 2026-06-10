package cn.zest.www.zestllm.admin.component;

import cn.zest.www.zestllm.admin.model.dto.InvokeCommand;
import cn.zest.www.zestllm.admin.service.LlmInvokeService;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import com.zestflow.executor.annotation.ZestComponent;
import com.zestflow.executor.annotation.ZestExecute;
import com.zestflow.executor.annotation.ZestParam;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 简化 invoke 元件，供 ZestFlow 多节点 DAG 使用。
 */
@ZestComponent("llmFlowInvokeHandler")
@RequiredArgsConstructor
public class LlmFlowInvokeHandler {

    private static final String DEFAULT_APP_KEY = "order-service";
    private static final String DEFAULT_CODE = "aiChat";
    private static final String DEFAULT_BEARER = "demo-token-123";
    private static final String TOOL_LOOP_CODE = "aiChatTools";

    private final LlmInvokeService llmInvokeService;

    @ZestExecute(value = "invokeByQuestion", name = "按 question 调用 AI Chat")
    public InvokeResponse invokeByQuestion(@ZestParam("question") String question,
                                           @ZestParam("bearerToken") String bearerToken) {
        return llmInvokeService.invoke(buildCommand(
                DEFAULT_APP_KEY,
                DEFAULT_CODE,
                Map.of("question", question != null ? question : "hello"),
                bearerToken));
    }

    @ZestExecute(value = "invokeDemoChat", name = "Demo AI Chat 调用（兼容旧链）")
    public InvokeResponse invokeDemoChat(@ZestParam("question") String question) {
        return invokeByQuestion(question, DEFAULT_BEARER);
    }

    @ZestExecute(value = "invokeToolLoopChat", name = "MCP Tool Loop 调用")
    public InvokeResponse invokeToolLoopChat(@ZestParam("question") String question,
                                             @ZestParam("bearerToken") String bearerToken) {
        return llmInvokeService.invoke(buildCommand(
                DEFAULT_APP_KEY,
                TOOL_LOOP_CODE,
                Map.of("question", question != null ? question : "search docs"),
                bearerToken));
    }

    private InvokeCommand buildCommand(String appKey, String code, Map<String, Object> inputs, String bearerToken) {
        InvokeRequest request = new InvokeRequest();
        request.setAppKey(appKey);
        request.setCode(code);
        request.setInputs(inputs);
        InvokeCommand command = new InvokeCommand();
        command.setBearerToken(bearerToken != null && !bearerToken.isBlank() ? bearerToken : DEFAULT_BEARER);
        command.setRequest(request);
        return command;
    }
}
