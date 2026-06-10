package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.common.api.KnowledgePrefetchSummary;
import cn.zest.www.zestllm.common.api.LearningLoopSummary;
import cn.zest.www.zestllm.common.api.RuntimeBackendSummary;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.KnowledgeRefConfig;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;
import cn.zest.www.zestllm.spi.profile.RuntimeBackendConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProfileIntegrationHelper {

    private final KnowledgeRetrievalAdapter knowledgeRetrievalAdapter;

    public RuntimeBackendSummary toRuntimeBackendSummary(RuntimeBackendConfig config) {
        if (config == null) {
            return null;
        }
        return RuntimeBackendSummary.builder()
                .type(config.getType())
                .baseUrl(config.getBaseUrl())
                .externalAppId(config.getExternalAppId())
                .protocol(config.getProtocol())
                .timeoutMs(config.getTimeoutMs())
                .build();
    }

    public LearningLoopSummary toLearningLoopSummary(LearningLoopConfig config) {
        if (config == null) {
            return LearningLoopSummary.builder().enabled(false).build();
        }
        return LearningLoopSummary.builder()
                .enabled(config.isEnabled())
                .evalDatasetRef(config.getEvalDatasetRef())
                .minPassRate(config.getMinPassRate())
                .probeBeforePublish(config.isProbeBeforePublish())
                .reviewRequired(config.isReviewRequired())
                .build();
    }

    public KnowledgePrefetchSummary prefetchKnowledge(AgentProfileDocument document, String runtimeMode,
                                                      String taskCode, String appKey, String traceId,
                                                      Map<String, Object> inputs) {
        KnowledgeRefConfig knowledge = ProfileExtensions.knowledge(document).orElse(null);
        if (knowledge == null || !knowledge.isEnabled()) {
            return null;
        }
        if ("external".equalsIgnoreCase(runtimeMode) && "external".equalsIgnoreCase(knowledge.getInjectMode())) {
            return KnowledgePrefetchSummary.builder()
                    .enabled(true)
                    .provider(knowledge.getProvider())
                    .chunkCount(0)
                    .preview("delegated to external runtime")
                    .build();
        }
        if ("none".equalsIgnoreCase(knowledge.getInjectMode())) {
            return null;
        }
        boolean hybrid = "hybrid".equalsIgnoreCase(runtimeMode)
                || "system_prefix".equalsIgnoreCase(knowledge.getInjectMode());
        if (!hybrid) {
            return null;
        }
        String query = extractQuery(inputs);
        KnowledgeRetrieveResult result = knowledgeRetrievalAdapter.retrieve(KnowledgeRetrieveRequest.builder()
                .traceId(traceId)
                .taskCode(taskCode)
                .appKey(appKey)
                .query(query)
                .knowledge(knowledge)
                .build());
        var chunks = result.getChunks().stream()
                .map(c -> KnowledgePrefetchSummary.KnowledgeChunkSummary.builder()
                        .content(c.getContent())
                        .score(c.getScore())
                        .source(c.getSource())
                        .build())
                .collect(Collectors.toList());
        String preview = chunks.stream()
                .map(KnowledgePrefetchSummary.KnowledgeChunkSummary::getContent)
                .filter(StringUtils::hasText)
                .limit(3)
                .collect(Collectors.joining("\n---\n"));
        return KnowledgePrefetchSummary.builder()
                .enabled(true)
                .provider(knowledge.getProvider())
                .chunkCount(chunks.size())
                .preview(preview)
                .chunks(chunks)
                .build();
    }

    public String injectKnowledgePrefix(String renderedPrompt, KnowledgePrefetchSummary prefetch) {
        if (prefetch == null || prefetch.getChunkCount() == 0 || !StringUtils.hasText(prefetch.getPreview())) {
            return renderedPrompt;
        }
        String block = "[Knowledge Context]\n" + prefetch.getPreview() + "\n[/Knowledge Context]\n\n";
        return block + (renderedPrompt != null ? renderedPrompt : "");
    }

    public String extractQuery(Map<String, Object> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return "";
        }
        for (String key : new String[]{"question", "query", "input", "text", "prompt"}) {
            Object value = inputs.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return inputs.toString();
    }
}
