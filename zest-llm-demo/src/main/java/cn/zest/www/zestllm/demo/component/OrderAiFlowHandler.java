package cn.zest.www.zestllm.demo.component;

import cn.zest.www.zestllm.flow.ZestLlmFlowNodeExecutor;
import com.zestflow.executor.annotation.ZestComponent;
import com.zestflow.executor.annotation.ZestExecute;
import com.zestflow.executor.annotation.ZestParam;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo 业务 Executor：通过 flow-adapter 调用 CP aiChat。
 */
@ZestComponent("orderAiFlowHandler")
@RequiredArgsConstructor
public class OrderAiFlowHandler {

    private final ZestLlmFlowNodeExecutor flowNodeExecutor;

    @ZestExecute(value = "chatViaCp", name = "经 CP 调用 AI Chat")
    public Map<String, Object> chatViaCp(@ZestParam("question") String question) {
        ZestLlmFlowNodeExecutor.FlowLlmResult result = flowNodeExecutor.execute(
                "aiChat",
                Map.of("question", question != null ? question : "hello"),
                null);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("traceId", result.getTraceId());
        view.put("output", result.getOutput());
        if (result.getOutput() != null && result.getOutput().get("answer") != null) {
            view.put("answer", result.getOutput().get("answer"));
        }
        return view;
    }
}
