package cn.zest.www.zestllm.admin.component;

import cn.zest.www.zestllm.admin.model.dto.PublishPromptCommand;
import cn.zest.www.zestllm.admin.model.vo.PromptPublishResultVO;
import cn.zest.www.zestllm.admin.service.PromptPublishService;
import com.zestflow.executor.annotation.ZestComponent;
import com.zestflow.executor.annotation.ZestExecute;
import com.zestflow.executor.annotation.ZestParam;
import lombok.RequiredArgsConstructor;

/**
 * Prompt 发布治理元件 — 供 ZestFlow 编排发布工作流。
 */
@ZestComponent("llmPromptHandler")
@RequiredArgsConstructor
public class LlmPromptHandler {

    private final PromptPublishService promptPublishService;

    /**
     * 发布指定 Prompt 版本（同 task 下其他已发布版本自动降级为 DRAFT）。
     *
     * @param command 发布命令（taskCode + version + operator）
     * @return 发布结果
     */
    @ZestExecute(value = "publishPrompt", name = "发布 Prompt 版本")
    public PromptPublishResultVO publishPrompt(@ZestParam("command") PublishPromptCommand command) {
        return promptPublishService.publish(command);
    }
}
