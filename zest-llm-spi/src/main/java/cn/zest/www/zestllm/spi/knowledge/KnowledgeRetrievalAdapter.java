package cn.zest.www.zestllm.spi.knowledge;

import cn.zest.www.zestllm.spi.model.HealthStatus;

/**
 * 知识检索 SPI（RAGFlow / Dify KB 等）。
 */
public interface KnowledgeRetrievalAdapter {

    String adapterId();

    KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request);

    HealthStatus health();
}
