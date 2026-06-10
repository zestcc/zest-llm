package cn.zest.www.zestllm.infra.knowledge;

import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult;
import cn.zest.www.zestllm.spi.model.HealthStatus;

public class NoopKnowledgeRetrievalAdapter implements KnowledgeRetrievalAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        return KnowledgeRetrieveResult.builder().build();
    }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("noop knowledge retrieval").build();
    }
}
