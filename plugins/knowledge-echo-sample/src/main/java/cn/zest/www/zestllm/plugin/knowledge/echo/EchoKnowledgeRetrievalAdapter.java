package cn.zest.www.zestllm.plugin.knowledge.echo;

import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.model.HealthStatus;

import java.util.List;

/**
 * 外置 SPI 样本：将 query 原样作为 chunk 返回，便于验证 plugins 目录加载。
 */
public class EchoKnowledgeRetrievalAdapter implements KnowledgeRetrievalAdapter {

    public EchoKnowledgeRetrievalAdapter() {
    }

    @Override
    public String adapterId() {
        return "echo-kb";
    }

    @Override
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        String query = request.getQuery() == null ? "" : request.getQuery();
        return KnowledgeRetrieveResult.builder()
                .chunks(List.of(KnowledgeRetrieveResult.KnowledgeChunk.builder()
                        .content("[echo-kb] " + query)
                        .score(1.0)
                        .source("echo-kb-sample")
                        .documentId("echo-1")
                        .build()))
                .build();
    }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("echo-kb sample plugin").build();
    }
}
