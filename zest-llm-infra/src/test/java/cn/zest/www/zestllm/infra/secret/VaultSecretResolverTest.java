package cn.zest.www.zestllm.infra.secret;

import cn.zest.www.zestllm.infra.config.VaultProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultSecretResolverTest {

    private static WireMockServer wireMock;
    private static VaultSecretResolver resolver;

    @BeforeAll
    static void start() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        wireMock.stubFor(get(urlEqualTo("/v1/secret/data/llm/litellm"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"data":{"data":{"apiKey":"vault-sk-123"}}}
                                """)));

        VaultProperties properties = new VaultProperties();
        properties.setAddress("http://localhost:" + wireMock.port());
        properties.setToken("test-token");
        properties.setMount("secret");
        resolver = new VaultSecretResolver(properties, new ObjectMapper(), RestClient.builder());
    }

    @AfterAll
    static void stop() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void resolve_readsFromVaultKv() {
        assertEquals("vault-sk-123", resolver.resolve("vault:llm/litellm#apiKey").orElseThrow());
    }

    @Test
    void resolve_returnsEmptyForNonVaultRef() {
        assertTrue(resolver.resolve("env:MISSING").isEmpty());
    }
}
