package cn.zest.www.zestllm.common.api;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class KnowledgePrefetchSummary {
    private boolean enabled;
    private String provider;
    private int chunkCount;
    private String preview;
    @Builder.Default
    private List<KnowledgeChunkSummary> chunks = new ArrayList<>();

    @Data
    @Builder
    public static class KnowledgeChunkSummary {
        private String content;
        private double score;
        private String source;
    }
}
