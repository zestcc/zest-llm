package cn.zest.www.zestllm.demo.controller;

import cn.zest.www.zestllm.agent.LlmAgentClient;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.PrepareResponse;
import cn.zest.www.zestllm.starter.config.ZestLlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE 流式 Demo（对标 OpenAI stream=true，推理直连 LiteLLM，不经 CP 转发 token 流）。
 */
@Slf4j
@RestController
@RequestMapping("/demo/order")
@RequiredArgsConstructor
public class OrderStreamController {

    private final ObjectProvider<LlmAgentClient> agentClientProvider;
    private final ZestLlmProperties properties;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "demo-stream");
        t.setDaemon(true);
        return t;
    });

    @GetMapping(value = "/methodA/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMethodA(@RequestParam String question) {
        SseEmitter emitter = new SseEmitter(120_000L);
        LlmAgentClient agentClient = agentClientProvider.getIfAvailable();
        if (agentClient == null || !agentClient.isEnabled()) {
            emitter.completeWithError(new IllegalStateException("Agent mode not enabled"));
            return emitter;
        }

        streamExecutor.submit(() -> {
            try {
                InvokeRequest request = new InvokeRequest();
                request.setAppKey(properties.getAppKey());
                request.setCode("aiChat");
                request.setInputs(Map.of("question", question));

                PrepareResponse prepared = agentClient.prepare(request);
                emitter.send(SseEmitter.event().name("prepare").data(Map.of(
                        "traceId", prepared.getTraceId(),
                        "promptVersion", prepared.getPromptVersion() != null ? prepared.getPromptVersion() : ""
                )));

                Map<String, Object> inputs = new HashMap<>();
                inputs.put("question", question);
                agentClient.executeStream(prepared, inputs, chunk -> {
                    try {
                        if (chunk.getDelta() != null && !chunk.getDelta().isBlank()) {
                            emitter.send(SseEmitter.event().name("delta").data(chunk.getDelta()));
                        }
                        if (chunk.isDone()) {
                            emitter.send(SseEmitter.event().name("done").data(Map.of(
                                    "traceId", chunk.getTraceId() != null ? chunk.getTraceId() : prepared.getTraceId()
                            )));
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                emitter.complete();
            } catch (Exception ex) {
                log.warn("Stream demo failed", ex);
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }
}
