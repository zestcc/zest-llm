package cn.zest.www.zestllm.demo.controller;

import cn.zest.www.zestllm.demo.facade.OrderAiFacade;
import cn.zest.www.zestllm.demo.model.AiChatResult;
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
}
