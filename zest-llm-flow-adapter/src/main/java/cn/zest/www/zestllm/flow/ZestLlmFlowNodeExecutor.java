package cn.zest.www.zestllm.flow;

import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * ZEST_LLM 流程节点执行器：通过 Control Plane {@code POST /v1/llm/invoke} 完成单次 AI 作业。
 */
@Slf4j
@RequiredArgsConstructor
public class ZestLlmFlowNodeExecutor {

    private static final String INVOKE_PATH = "/v1/llm/invoke";

    private final RestClient restClient;
    private final ZestLlmFlowProperties properties;

    public FlowLlmResult execute(String code, Map<String, Object> inputs, Map<String, Object> context) {
        InvokeRequest request = new InvokeRequest();
        request.setAppKey(properties.getAppKey());
        request.setCode(code);
        request.setInputs(inputs);
        request.setContext(context);

        log.debug("Flow invoke code={} appKey={}", code, properties.getAppKey());
        InvokeResponse response = restClient.post()
                .uri(INVOKE_PATH)
                .body(request)
                .retrieve()
                .body(InvokeResponse.class);

        if (response == null) {
            throw new IllegalStateException("Control Plane invoke returned empty response");
        }
        if (!response.isSuccess()) {
            throw new IllegalStateException("LLM invoke failed: " + response.getErrorMessage());
        }

        return FlowLlmResult.builder()
                .traceId(response.getTraceId())
                .output(response.getOutput())
                .build();
    }

    @Data
    @Builder
    public static class FlowLlmResult {
        private String traceId;
        private Map<String, Object> output;
    }
}
