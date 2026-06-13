package cn.zest.www.zestllm.demo.controller;

import cn.zest.www.zestllm.demo.facade.OrderAiFacade;
import cn.zest.www.zestllm.demo.flow.DemoFlowChainBootstrap;
import cn.zest.www.zestllm.demo.model.AiChatResult;
import com.zestflow.common.exception.ChainExecutionException;
import com.zestflow.common.model.dto.ChainExecuteResultDTO;
import com.zestflow.executor.http.ChainGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo/order")
@RequiredArgsConstructor
public class OrderDemoController {

    private final OrderAiFacade orderAiFacade;
    private final ChainGateway chainGateway;

    @GetMapping("/methodA")
    public Map<String, Object> methodA(@RequestParam Long orderId,
                                       @RequestParam String question) {
        AiChatResult ai = orderAiFacade.aiChat(question, new AiChatResult());
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("orderId", orderId);
        view.put("answer", ai.getAnswer());
        view.put("confidence", ai.getConfidence());
        view.put("tags", ai.getTags());
        view.put("needManualReview", ai.getNeedManualReview());
        view.put("traceId", ai.getTraceId());
        return view;
    }

    @GetMapping("/flowChat")
    public Map<String, Object> flowChat(@RequestParam Long orderId,
                                        @RequestParam String question) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("orderId", orderId);
        view.put("mode", "zestflow");
        view.put("chainCode", DemoFlowChainBootstrap.CHN_DEMO_ORDER_CHAT);
        ChainExecuteResultDTO chainResult = runDemoChain(question);
        mergeChainResult(view, chainResult);
        return view;
    }

    private ChainExecuteResultDTO runDemoChain(String question) {
        try {
            return chainGateway.execute(
                    DemoFlowChainBootstrap.CHN_DEMO_ORDER_CHAT,
                    Map.of("question", question != null ? question : "hello"));
        } catch (ChainExecutionException ex) {
            ChainExecuteResultDTO partial = ex.getResult();
            if (partial != null && hasChainPayload(partial)) {
                return partial;
            }
            throw ex;
        }
    }

    private boolean hasChainPayload(ChainExecuteResultDTO chainResult) {
        if (chainResult.getReturnValue() != null || chainResult.getFinalReturnValue() != null) {
            return true;
        }
        if (chainResult.getResultData() != null && !chainResult.getResultData().isEmpty()) {
            return true;
        }
        return chainResult.getNodeResults() != null && !chainResult.getNodeResults().isEmpty();
    }

    private void mergeChainResult(Map<String, Object> view, ChainExecuteResultDTO chainResult) {
        Object payload = chainResult.getReturnValue();
        if (payload == null) {
            payload = chainResult.getFinalReturnValue();
        }
        if (payload == null && chainResult.getResultData() != null && !chainResult.getResultData().isEmpty()) {
            payload = chainResult.getResultData();
        }
        if (payload == null && chainResult.getNodeResults() != null && !chainResult.getNodeResults().isEmpty()) {
            var lastNode = chainResult.getNodeResults().get(chainResult.getNodeResults().size() - 1);
            payload = lastNode.getReturnValue();
            if (payload == null && lastNode.getOutputData() != null && !lastNode.getOutputData().isEmpty()) {
                payload = lastNode.getOutputData();
            }
        }
        if (payload instanceof Map<?, ?> resultMap) {
            resultMap.forEach((key, value) -> view.put(String.valueOf(key), value));
        } else if (payload != null) {
            view.put("result", payload);
        }
    }
}
