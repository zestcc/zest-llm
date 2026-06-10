package cn.zest.www.zestllm.admin.component;

import cn.zest.www.zestllm.flow.ZestLlmFlowNodeExecutor;
import com.zestflow.executor.annotation.ZestComponent;
import com.zestflow.executor.annotation.ZestExecute;
import com.zestflow.executor.annotation.ZestParam;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 通过 flow-adapter 调用 CP invoke，供外部编排或多 AI 节点复用。
 */
@ZestComponent("zestLlmFlowHandler")
@RequiredArgsConstructor
public class ZestLlmFlowComponent {

    private final ZestLlmFlowNodeExecutor flowNodeExecutor;

    @ZestExecute(value = "invokeByCode", name = "按 Task Code 调用 CP")
    public ZestLlmFlowNodeExecutor.FlowLlmResult invokeByCode(@ZestParam("code") String code,
                                                              @ZestParam("inputs") Map<String, Object> inputs) {
        return flowNodeExecutor.execute(code, inputs, null);
    }
}
