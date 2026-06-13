package cn.zest.www.zestllm.infra.knowledge;

import cn.zest.www.zestllm.infra.config.HttpKnowledgeProperties;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.profile.KnowledgeRefConfig;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HttpKnowledgeRetrievalAdapterTest {

    @Mock
    private SecretResolver secretResolver;

    @Test
    void adapterId_isHttpKnowledge() {
        HttpKnowledgeRetrievalAdapter adapter = new HttpKnowledgeRetrievalAdapter(
                new HttpKnowledgeProperties(), secretResolver, new ObjectMapper());
        assertThat(adapter.adapterId()).isEqualTo("http-knowledge");
    }

    @Test
    void retrieve_returnsEmptyWhenKnowledgeDisabled() {
        HttpKnowledgeRetrievalAdapter adapter = new HttpKnowledgeRetrievalAdapter(
                new HttpKnowledgeProperties(), secretResolver, new ObjectMapper());
        var result = adapter.retrieve(KnowledgeRetrieveRequest.builder().query("q").build());
        assertThat(result.getChunks()).isEmpty();
    }

    @Test
    void health_reportsUnreachableForBadPort() {
        HttpKnowledgeProperties properties = new HttpKnowledgeProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setConnectTimeoutMs(300);
        properties.setReadTimeoutMs(300);
        HttpKnowledgeRetrievalAdapter adapter = new HttpKnowledgeRetrievalAdapter(
                properties, secretResolver, new ObjectMapper());
        var health = adapter.health();
        assertThat(health.isUp()).isFalse();
    }

    @Test
    void retrieve_swallowsHttpErrors() {
        HttpKnowledgeProperties properties = new HttpKnowledgeProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setConnectTimeoutMs(300);
        properties.setReadTimeoutMs(300);
        HttpKnowledgeRetrievalAdapter adapter = new HttpKnowledgeRetrievalAdapter(
                properties, secretResolver, new ObjectMapper());
        KnowledgeRefConfig knowledge = new KnowledgeRefConfig();
        knowledge.setEnabled(true);
        knowledge.setTopK(3);
        var result = adapter.retrieve(KnowledgeRetrieveRequest.builder()
                .query("test")
                .knowledge(knowledge)
                .build());
        assertThat(result.getChunks()).isEmpty();
    }
}
