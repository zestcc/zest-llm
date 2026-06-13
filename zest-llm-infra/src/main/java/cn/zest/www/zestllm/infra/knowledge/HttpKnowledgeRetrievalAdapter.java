package cn.zest.www.zestllm.infra.knowledge;

import cn.zest.www.zestllm.infra.config.HttpKnowledgeProperties;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.profile.KnowledgeRefConfig;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic HTTP knowledge retrieval adapter (plug-and-play SPI).
 */
@Slf4j
@RequiredArgsConstructor
public class HttpKnowledgeRetrievalAdapter implements KnowledgeRetrievalAdapter {

    private final HttpKnowledgeProperties properties;
    private final SecretResolver secretResolver;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "http-knowledge";
    }

    @Override
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        KnowledgeRefConfig knowledge = request.getKnowledge();
        if (knowledge == null || !knowledge.isEnabled()) {
            return KnowledgeRetrieveResult.builder().build();
        }
        String baseUrl = StringUtils.hasText(knowledge.getBaseUrl()) ? knowledge.getBaseUrl() : properties.getBaseUrl();
        String apiKey = resolveApiKey(knowledge);
        try {
            RestClient client = buildClient(baseUrl, apiKey);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("query", request.getQuery() != null ? request.getQuery() : "");
            body.put("top_k", knowledge.getTopK());
            body.put("score_threshold", knowledge.getScoreThreshold());
            if (knowledge.getDatasetIds() != null && !knowledge.getDatasetIds().isEmpty()) {
                ArrayNode datasets = body.putArray("dataset_ids");
                knowledge.getDatasetIds().forEach(datasets::add);
            }
            String raw = client.post()
                    .uri(properties.getRetrievePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            return KnowledgeRetrieveResult.builder().chunks(parseChunks(raw, knowledge.getScoreThreshold())).build();
        } catch (Exception ex) {
            log.warn("HTTP knowledge retrieve failed query={}", request.getQuery(), ex);
            return KnowledgeRetrieveResult.builder().build();
        }
    }

    @Override
    public HealthStatus health() {
        try {
            RestClient client = buildClient(properties.getBaseUrl(), properties.getApiKey());
            client.get().uri(properties.getHealthPath()).retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("http-knowledge reachable").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message("http-knowledge unreachable: " + ex.getMessage()).build();
        }
    }

    private List<KnowledgeRetrieveResult.KnowledgeChunk> parseChunks(String raw, double threshold) {
        List<KnowledgeRetrieveResult.KnowledgeChunk> chunks = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            return chunks;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.has("chunks") ? root.get("chunks")
                    : root.has("data") ? root.get("data") : root;
            if (data.isArray()) {
                for (JsonNode item : data) {
                    double score = item.path("score").asDouble(0);
                    if (score > 0 && score < threshold) {
                        continue;
                    }
                    String content = item.path("content").asText(item.path("text").asText(""));
                    if (!StringUtils.hasText(content)) {
                        continue;
                    }
                    chunks.add(KnowledgeRetrieveResult.KnowledgeChunk.builder()
                            .content(content)
                            .score(score)
                            .source(item.path("source").asText(""))
                            .documentId(item.path("document_id").asText(""))
                            .build());
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to parse http-knowledge response");
        }
        return chunks;
    }

    private String resolveApiKey(KnowledgeRefConfig knowledge) {
        if (knowledge != null && StringUtils.hasText(knowledge.getSecretRef())) {
            return secretResolver.resolve(knowledge.getSecretRef()).orElse(properties.getApiKey());
        }
        return properties.getApiKey();
    }

    private RestClient buildClient(String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(apiKey)) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }
}
