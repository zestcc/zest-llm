package cn.zest.www.zestllm.plugin.knowledge.difykb;

import cn.zest.www.zestllm.plugin.dify.common.DifyProperties;
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

@Slf4j
@RequiredArgsConstructor
public class DifyKbKnowledgeRetrievalAdapter implements KnowledgeRetrievalAdapter {

    private final DifyProperties properties;
    private final SecretResolver secretResolver;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "dify-kb";
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
            body.put("query", query != null ? query : "");
            ObjectNode retrievalModel = objectMapper.createObjectNode();
            retrievalModel.put("search_method", "semantic_search");
            retrievalModel.put("top_k", knowledge.getTopK());
            retrievalModel.put("score_threshold_enabled", knowledge.getScoreThreshold() > 0);
            retrievalModel.put("score_threshold", knowledge.getScoreThreshold());
            body.set("retrieval_model", retrievalModel);
            String raw = client.post()
                    .uri("/v1/datasets/{datasetId}/retrieve", datasetId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            return parseRecords(raw, knowledge.getScoreThreshold());
        } catch (Exception ex) {
            log.warn("Dify KB retrieve failed dataset={} query={}", datasetId, query, ex);
            return List.of();
        }
    }

    private List<KnowledgeRetrieveResult.KnowledgeChunk> parseRecords(String raw, double threshold) {
        List<KnowledgeRetrieveResult.KnowledgeChunk> chunks = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            return chunks;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode records = root.has("records") ? root.get("records") : root.get("data");
            if (records == null || !records.isArray()) {
                return chunks;
            }
            appendRecords(chunks, (ArrayNode) records, threshold);
        } catch (Exception ex) {
            log.debug("Failed to parse Dify KB response");
        }
        return chunks;
    }

    private void appendRecords(List<KnowledgeRetrieveResult.KnowledgeChunk> chunks, ArrayNode records, double threshold) {
        for (JsonNode item : records) {
            double score = item.path("score").asDouble(0);
            if (score < threshold && score > 0) {
                continue;
            }
            JsonNode segment = item.has("segment") ? item.get("segment") : item;
            String content = segment.path("content").asText("");
            if (!StringUtils.hasText(content)) {
                continue;
            }
            JsonNode document = segment.path("document");
            chunks.add(KnowledgeRetrieveResult.KnowledgeChunk.builder()
                    .content(content)
                    .score(score)
                    .source(document.path("name").asText(segment.path("document_name").asText("")))
                    .documentId(document.path("id").asText(segment.path("document_id").asText("")))
                    .build());
        }
    }

    @Override
    public HealthStatus health() {
        try {
            RestClient client = buildClient(properties.getBaseUrl(), properties.getApiKey());
            client.get().uri("/v1/datasets?page=1&limit=1").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("Dify KB reachable").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message("Dify KB unreachable: " + ex.getMessage()).build();
        }
    }

    public HealthStatus health(KnowledgeRefConfig knowledge) {
        String baseUrl = knowledge != null && StringUtils.hasText(knowledge.getBaseUrl())
                ? knowledge.getBaseUrl() : properties.getBaseUrl();
        try {
            RestClient client = buildClient(baseUrl, resolveApiKey(knowledge));
            client.get().uri("/v1/datasets?page=1&limit=1").retrieve().toBodilessEntity();
            return HealthStatus.builder().up(true).message("Dify KB " + baseUrl + " OK").build();
        } catch (Exception ex) {
            return HealthStatus.builder().up(false).message("Dify KB health failed: " + ex.getMessage()).build();
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
