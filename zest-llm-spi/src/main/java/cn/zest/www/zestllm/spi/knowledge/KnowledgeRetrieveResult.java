package cn.zest.www.zestllm.spi.knowledge;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class KnowledgeRetrieveResult {

    @Builder.Default
    private List<KnowledgeChunk> chunks = new ArrayList<>();

    @Data
    @Builder
    public static class KnowledgeChunk {
        private String content;
        private double score;
        private String source;
        private String documentId;
    }
}
