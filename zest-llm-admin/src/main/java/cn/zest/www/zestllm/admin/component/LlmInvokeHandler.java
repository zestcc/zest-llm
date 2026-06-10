package cn.zest.www.zestllm.admin.component;

import cn.zest.www.zestllm.admin.model.dto.InvokeCommand;
import cn.zest.www.zestllm.admin.service.LlmInvokeService;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import com.zestflow.executor.annotation.ZestComponent;
import com.zestflow.executor.annotation.ZestExecute;
import com.zestflow.executor.annotation.ZestParam;
import lombok.RequiredArgsConstructor;

/**
 * LLM 运行时调用元件 — 供 ZestFlow 编排 invokeByCommand 节点。
 */
@ZestComponent("llmInvokeHandler")
@RequiredArgsConstructor
public class LlmInvokeHandler {

    private final LlmInvokeService llmInvokeService;

    /**
     * 按作业 code 执行 LLM 调用（鉴权 + Prompt 渲染 + 模型网关 + Execution 落库）。
     *
     * @param command 含 Bearer token 与 InvokeRequest 的调用命令
     * @return 调用结果
     */
    @ZestExecute(value = "invokeByCommand", name = "按 command 调用 LLM")
    public InvokeResponse invokeByCommand(@ZestParam("command") InvokeCommand command) {
        return llmInvokeService.invoke(command);
    }
}
