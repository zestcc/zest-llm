package cn.zest.www.zestllm.plugin.knowledge.difykb;

import cn.zest.www.zestllm.plugin.dify.common.DifyProperties;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult;
import cn.zest.www.zestllm.spi.profile.KnowledgeRefConfig;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DifyKbKnowledgeRetrievalAdapterTest {

    private static WireMockServer wireMock;
    private static DifyKbKnowledgeRetrievalAdapter adapter;

    @BeforeAll
    static void start() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        wireMock.stubFor(post(urlEqualTo("/v1/datasets/ds-1/retrieve"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "records": [
                                    {
                                      "score": 0.92,
                                      "segment": {
                                        "content": "runbook step 1",
                                        "document": {"id": "doc-1", "name": "ops-runbook"}
                                      }
                                    }
                                  ]
                                }
                                """)));
        DifyProperties properties = new DifyProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.port());
        properties.setApiKey("test-key");
        SecretResolver secretResolver = mock(SecretResolver.class);
        when(secretResolver.resolve("any")).thenReturn(Optional.empty());
        adapter = new DifyKbKnowledgeRetrievalAdapter(properties, secretResolver, new ObjectMapper());
    }

    @AfterAll
    static void stop() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void adapterId_isDifyKb() {
        assertEquals("dify-kb", adapter.adapterId());
    }

    @Test
    void retrieve_returnsChunksFromDifyApi() {
        KnowledgeRefConfig knowledge = new KnowledgeRefConfig();
        knowledge.setEnabled(true);
        knowledge.setDatasetIds(List.of("ds-1"));
        knowledge.setTopK(5);
        knowledge.setScoreThreshold(0.5);

        KnowledgeRetrieveResult result = adapter.retrieve(KnowledgeRetrieveRequest.builder()
                .query("how to restart")
                .knowledge(knowledge)
                .build());

        assertEquals(1, result.getChunks().size());
        assertEquals("runbook step 1", result.getChunks().get(0).getContent());
        assertTrue(result.getChunks().get(0).getScore() >= 0.9);
    }

    @Test
    void retrieve_skipsWhenDisabled() {
        KnowledgeRefConfig knowledge = new KnowledgeRefConfig();
        knowledge.setEnabled(false);

        KnowledgeRetrieveResult result = adapter.retrieve(KnowledgeRetrieveRequest.builder()
                .query("q")
                .knowledge(knowledge)
                .build());

        assertTrue(result.getChunks() == null || result.getChunks().isEmpty());
    }
}
