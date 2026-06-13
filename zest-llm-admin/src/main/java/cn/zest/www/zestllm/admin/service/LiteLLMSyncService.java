package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import cn.zest.www.zestllm.admin.model.vo.LiteLLMSyncResultVO;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.infra.config.LiteLLMProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiteLLMSyncService {

    private final LlmGatewayModelRepo gatewayModelRepo;
    private final SecretRefManageService secretRefManageService;
    private final LiteLLMProperties liteLLMProperties;
    private final ObjectMapper objectMapper;

    public LiteLLMSyncResultVO syncAll() {
        List<LlmGatewayModelDO> models = gatewayModelRepo.findAllActive();
        int synced = 0;
        int failed = 0;
        for (LlmGatewayModelDO model : models) {
            if (syncModel(model)) {
                synced++;
            } else {
                failed++;
            }
        }
        return LiteLLMSyncResultVO.builder()
                .total(models.size())
                .synced(synced)
                .failed(failed)
                .message(String.format("synced=%d failed=%d", synced, failed))
                .build();
    }

    public boolean syncModel(LlmGatewayModelDO model) {
        try {
            RestClient client = buildClient();
            ObjectNode body = buildModelPayload(model);
            client.post()
                    .uri("/model/new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .toBodilessEntity();
            markSyncStatus(model, "SYNCED");
            return true;
        } catch (Exception ex) {
            log.warn("LiteLLM sync failed model={}: {}", model.getModelName(), ex.getMessage());
            markSyncStatus(model, "FAILED");
            return false;
        }
    }

    public boolean healthCheck(String modelName) {
        try {
            RestClient client = buildClient();
            String raw = client.get()
                    .uri("/model/info")
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(raw)) {
                return false;
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data.isArray()) {
                for (JsonNode item : data) {
                    if (modelName.equals(item.path("model_name").asText())) {
                        return true;
                    }
                }
            } else if (data.isObject()) {
                Iterator<String> names = data.fieldNames();
                while (names.hasNext()) {
                    if (modelName.equals(names.next())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            log.debug("LiteLLM health check failed model={}: {}", modelName, ex.getMessage());
            return false;
        }
    }

    ObjectNode buildModelPayload(LlmGatewayModelDO model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model_name", model.getModelName());
        ObjectNode params = objectMapper.createObjectNode();
        params.put("model", model.getUpstreamModel());
        if (StringUtils.hasText(model.getApiBase())) {
            params.put("api_base", model.getApiBase());
        }
        String apiKey = secretRefManageService.resolveLiteLLMApiKey(model.getApiKeySecretRef());
        if (StringUtils.hasText(apiKey)) {
            params.put("api_key", apiKey);
        }
        if (StringUtils.hasText(model.getExtraJson())) {
            try {
                JsonNode extra = objectMapper.readTree(model.getExtraJson());
                if (extra.isObject()) {
                    extra.fields().forEachRemaining(entry -> params.set(entry.getKey(), entry.getValue()));
                }
            } catch (Exception ex) {
                log.debug("Ignoring invalid extra_json for model={}", model.getModelName());
            }
        }
        body.set("litellm_params", params);
        return body;
    }

    private void markSyncStatus(LlmGatewayModelDO model, String status) {
        model.setSyncStatus(status);
        model.setLastSyncAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        gatewayModelRepo.update(model);
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(liteLLMProperties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(Math.min(liteLLMProperties.getReadTimeoutMs(), 30000)));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(liteLLMProperties.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(liteLLMProperties.getApiKey())) {
            builder.defaultHeader("Authorization", "Bearer " + liteLLMProperties.getApiKey());
        }
        return builder.build();
    }
}
