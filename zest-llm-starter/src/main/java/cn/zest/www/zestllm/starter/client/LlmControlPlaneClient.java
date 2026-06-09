package cn.zest.www.zestllm.starter.client;

import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.api.MethodRegistryRequest;
import cn.zest.www.zestllm.common.api.PrepareRequest;
import cn.zest.www.zestllm.common.api.PrepareResponse;
import cn.zest.www.zestllm.common.api.ReportRequest;
import cn.zest.www.zestllm.starter.config.ZestLlmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
public class LlmControlPlaneClient {

    private static final String INVOKE_PATH = "/v1/llm/invoke";
    private static final String PREPARE_PATH = "/v1/llm/prepare";
    private static final String REPORT_PATH = "/v1/llm/report";
    private static final String REGISTRY_PATH = "/v1/registry/methods";

    private final ZestLlmProperties properties;
    private final RestClient restClient;

    public LlmControlPlaneClient(ZestLlmProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getControlPlaneUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (properties.getAuthToken() != null && !properties.getAuthToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getAuthToken());
        }
        this.restClient = builder.build();
    }

    public InvokeResponse invoke(InvokeRequest request) {
        fillAppKey(request);
        log.debug("Invoking LLM code={} appKey={}", request.getCode(), request.getAppKey());
        return restClient.post()
                .uri(INVOKE_PATH)
                .body(request)
                .retrieve()
                .body(InvokeResponse.class);
    }

    public PrepareResponse prepare(InvokeRequest request) {
        fillAppKey(request);
        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setAppKey(request.getAppKey());
        prepareRequest.setCode(request.getCode());
        prepareRequest.setBizId(request.getBizId());
        prepareRequest.setInputs(request.getInputs());
        prepareRequest.setContext(request.getContext());
        prepareRequest.setOptions(request.getOptions());
        log.debug("Prepare LLM code={} appKey={}", request.getCode(), request.getAppKey());
        return restClient.post()
                .uri(PREPARE_PATH)
                .body(prepareRequest)
                .retrieve()
                .body(PrepareResponse.class);
    }

    public void report(ReportRequest request) {
        if (request.getAppKey() == null) {
            request.setAppKey(properties.getAppKey());
        }
        log.debug("Report LLM traceId={} status={}", request.getTraceId(), request.getStatus());
        restClient.post()
                .uri(REPORT_PATH)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void registerMethods(MethodRegistryRequest request) {
        if (request.getAppKey() == null) {
            request.setAppKey(properties.getAppKey());
        }
        log.info("Registering {} methods for appKey={}",
                request.getMethods() != null ? request.getMethods().size() : 0,
                request.getAppKey());
        restClient.post()
                .uri(REGISTRY_PATH)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void fillAppKey(InvokeRequest request) {
        if (request.getAppKey() == null) {
            request.setAppKey(properties.getAppKey());
        }
    }
}
