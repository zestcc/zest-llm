package cn.zest.www.zestllm.infra.knowledge;

import cn.zest.www.zestllm.infra.config.RagflowProperties;
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
 * RAGFlow 知识检索（对标 RAGFlow retrieval API）。
 */
@Slf4j
@RequiredArgsConstructor
public class RagflowKnowledgeRetrievalAdapter implements KnowledgeRetrievalAdapter {

    private final RagflowProperties properties;
    private final SecretResolver secretResolver;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "ragflow";
    }

    @Override
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        KnowledgeRefConfig knowledge = request.getKnowledge();
        if (knowledge == null || !knowledge.isEnabled()) {
            return KnowledgeRetrieveResult.builder().build();
        }
        String baseUrl = StringUtils.hasText(knowledge.getBaseUrl()) ? knowledge.getBaseUrl() : properties.getBaseUrl();
        String apiKey = resolveApiKey(knowledge);
        List<KnowledgeRetrieveResult.KnowledgeChunk> chunks = new ArrayList<>();
        for (String datasetId : knowledge.getDatasetIds()) {
            chunks.addAll(retrieveDataset(baseUrl, apiKey, datasetId, request.getQuery(), knowledge));
        }
        chunks.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (chunks.size() > knowledge.getTopK()) {
            chunks = chunks.subList(0, knowledge.getTopK());
        }
        return KnowledgeRetrieveResult.builder().chunks(chunks).build();
    }

    private List<KnowledgeRetrieveResult.KnowledgeChunk> retrieveDataset(String baseUrl, String apiKey,
                                                                         String datasetId, String query,
                                                                         KnowledgeRefConfig knowledge) {
        try {
            RestClient client = buildClient(baseUrl, apiKey);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("question", query != null ? query : "");
            body.put("kb_id", datasetId);
            body.put("top_k", knowledge.getTopK());
            body.put("similarity_threshold", knowledge.getScoreThreshold());
            String raw = client.post()
                    .uri("/api/v1/retrieval")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            return parseChunks(raw, knowledge.getScoreThreshold());
        } catch (Exception ex) {
            log.warn("RAGFlow retrieve failed dataset={} query={}", datasetId, query, ex);
            return List.of();
        }
    }

    private List<KnowledgeRetrieveResult.KnowledgeChunk> parseChunks(String raw, double threshold) {
        List<KnowledgeRetrieveResult.KnowledgeChunk> chunks = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            return chunks;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data.isArray()) {
                appendChunks(chunks, (ArrayNode) data, threshold);
            } else if (data.has("chunks")) {
                appendChunks(chunks, (ArrayNode) data.get("chunks"), threshold);
            } else if (data.has("documents")) {
                appendChunks(chunks, (ArrayNode) data.get("documents"), threshold);
            }
        } catch (Exception ex) {
            log.debug("Failed to parse RAGFlow response");
        }
        return chunks;
    }

    private void appendChunks(List<KnowledgeRetrieveResult.KnowledgeChunk> chunks, ArrayNode array, double threshold) {
        for (JsonNode item : array) {
            double score = item.path("score").asDouble(item.path("similarity").asDouble(0));
            if (score < threshold && score > 0) {
                continue;
            }
            String content = item.path("content").asText(item.path("text").asText(""));
            if (!StringUtils.hasText(content)) {
                continue;
            }
            chunks.add(KnowledgeRetrieveResult.KnowledgeChunk.builder()
                    .content(content)
                    .score(score)
                    .source(item.path("source").asText(item.path("doc_name").asText("")))
                    .documentId(item.path("document_id").asText(item.path("doc_id").asText("")))
                    .build());
        }
    }

    @Override
    public HealthStatus health() {
        try {
            RestClient client = buildClient(properties.getBaseUrl(), properties.getApiKey());
            client.get().uri("/v1/system/healthz").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("RAGFlow reachable").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message("RAGFlow unreachable: " + ex.getMessage()).build();
        }
    }

    public HealthStatus health(KnowledgeRefConfig knowledge) {
        String baseUrl = knowledge != null && StringUtils.hasText(knowledge.getBaseUrl())
                ? knowledge.getBaseUrl() : properties.getBaseUrl();
        try {
            RestClient client = buildClient(baseUrl, resolveApiKey(knowledge));
            client.get().uri("/v1/system/healthz").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("RAGFlow " + baseUrl + " OK").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message("RAGFlow health failed: " + ex.getMessage()).build();
        }
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
