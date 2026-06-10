package cn.zest.www.zestllm.admin.controller.flow;

import cn.zest.www.zestllm.admin.flow.LlmFlowChainBootstrap;
import cn.zest.www.zestllm.admin.model.request.FlowAiChatRequest;
import com.zestflow.common.model.Result;
import com.zestflow.executor.annotation.ZestChain;
import com.zestflow.executor.http.ChainGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * ZestFlow Mode3 薄入口：{@code @ZestChain} + {@link ChainGateway}，业务在 DAG 元件中执行。
 */
@RestController
@RequestMapping("/api/flow")
@RequiredArgsConstructor
public class LlmFlowGatewayController {

    private static final String DEFAULT_BEARER = "demo-token-123";

    private final ChainGateway chainGateway;

    @PostMapping("/ai-chat")
    @ZestChain("zestllm.flow.aiChat")
    public Result<Object> aiChat(@RequestBody FlowAiChatRequest request) {
        return Result.success(chainGateway.executeByKey(LlmFlowChainBootstrap.CHN_AI_CHAT, buildAiChatParams(request)));
    }

    @PostMapping("/invoke-audit")
    @ZestChain("zestllm.flow.invokeAudit")
    public Result<Object> invokeAudit(@RequestBody FlowAiChatRequest request) {
        return Result.success(chainGateway.executeByKey(LlmFlowChainBootstrap.CHN_INVOKE_AUDIT, buildAiChatParams(request)));
    }

    @PostMapping("/flow-node")
    @ZestChain("zestllm.flow.node")
    public Result<Object> flowNode(@RequestBody FlowNodeRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", request.getCode() != null ? request.getCode() : "aiChat");
        params.put("inputs", request.getInputs() != null ? request.getInputs() : Map.of("question", "hello"));
        return Result.success(chainGateway.executeByKey(LlmFlowChainBootstrap.CHN_FLOW_NODE, params));
    }

    @PostMapping("/tool-loop")
    @ZestChain("zestllm.flow.toolLoop")
    public Result<Object> toolLoop(@RequestBody FlowAiChatRequest request) {
        return Result.success(chainGateway.executeByKey(LlmFlowChainBootstrap.CHN_TOOL_LOOP, buildAiChatParams(request)));
    }

    private Map<String, Object> buildAiChatParams(FlowAiChatRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("question", request.getQuestion() != null ? request.getQuestion() : "hello");
        params.put("bearerToken", request.getBearerToken() != null ? request.getBearerToken() : DEFAULT_BEARER);
        return params;
    }

    @lombok.Data
    public static class FlowNodeRequest {
        private String code;
        private Map<String, Object> inputs;
    }
}
