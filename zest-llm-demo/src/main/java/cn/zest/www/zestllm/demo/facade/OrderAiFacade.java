package cn.zest.www.zestllm.demo.facade;

import cn.zest.www.zestllm.demo.model.AiChatResult;
import cn.zest.www.zestllm.starter.annotation.AiInput;
import cn.zest.www.zestllm.starter.annotation.AiOutput;
import cn.zest.www.zestllm.starter.annotation.ZestLLM;
import org.springframework.stereotype.Component;

@Component
public class OrderAiFacade {

    @ZestLLM(code = "aiChat", timeoutMs = 30000, retry = 1)
    public AiChatResult aiChat(@AiInput("question") String question, @AiOutput AiChatResult result) {
        if (result.getConfidence() != null && result.getConfidence() < 0.8) {
            result.setNeedManualReview(true);
        }
        return result;
    }
}
