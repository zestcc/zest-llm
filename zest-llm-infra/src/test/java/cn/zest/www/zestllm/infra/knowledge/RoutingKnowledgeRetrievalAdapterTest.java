package cn.zest.www.zestllm.infra.knowledge;

import cn.zest.www.zestllm.infra.config.LlmAdapterProperties;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterDescriptor;
import cn.zest.www.zestllm.infra.plugin.ExternalAdapterRegistry;
import cn.zest.www.zestllm.plugin.noop.NoopKnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveRequest;
import cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingKnowledgeRetrievalAdapterTest {

    @Test
    void shouldRouteToExternalAdapterWhenConfigured() {
        ExternalAdapterRegistry registry = new ExternalAdapterRegistry();
        registry.register(ExternalAdapterDescriptor.builder()
                .spiType("knowledge-retrieval")
                .pluginId("echo-kb")
                .pluginName("echo-kb")
                .source(ExternalAdapterRegistry.SOURCE_EXTERNAL)
                .instanceSupplier(() -> new StubEchoAdapter())
                .build());

        LlmAdapterProperties props = new LlmAdapterProperties();
        props.setKnowledgeRetrieval("echo-kb");

        RoutingKnowledgeRetrievalAdapter routing = new RoutingKnowledgeRetrievalAdapter(
                props, registry, List.of(new NoopKnowledgeRetrievalAdapter()));

        assertThat(routing.adapterId()).isEqualTo("echo-kb");
        assertThat(routing.retrieve(KnowledgeRetrieveRequest.builder().query("hello").build())
                .getChunks().get(0).getContent()).contains("hello");
    }

    static class StubEchoAdapter implements KnowledgeRetrievalAdapter {
        @Override
        public String adapterId() {
            return "echo-kb";
        }

        @Override
        public cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
            return cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult.builder()
                    .chunks(List.of(cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrieveResult.KnowledgeChunk.builder()
                            .content("ext:" + request.getQuery())
                            .score(1.0)
                            .build()))
                    .build();
        }

        @Override
        public HealthStatus health() {
            return HealthStatus.builder().up(true).message("ok").build();
        }
    }
}
