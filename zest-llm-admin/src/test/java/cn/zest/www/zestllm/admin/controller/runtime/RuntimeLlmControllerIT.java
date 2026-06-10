package cn.zest.www.zestllm.admin.controller.runtime;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("it")
class RuntimeLlmControllerIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("zest_llm")
            .withUsername("test")
            .withPassword("test");

    static WireMockServer wireMock;

    static {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        wireMock.stubFor(get(urlEqualTo("/health/liveliness"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "choices": [{"message": {"content": "{\\"answer\\":\\"integration-ok\\"}"}}],
                                  "usage": {"prompt_tokens": 12, "completion_tokens": 8}
                                }
                                """)));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("zest.llm.litellm.base-url", () -> "http://localhost:" + wireMock.port());
    }

    @Test
    void invoke_withValidToken_returnsSuccessAndTraceId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("demo-token-123");

        Map<String, Object> body = Map.of(
                "appKey", "order-service",
                "code", "aiChat",
                "inputs", Map.of("question", "hello it")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/v1/llm/invoke", new HttpEntity<>(body, headers), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertNotNull(response.getBody().get("traceId"));
        assertTrue(response.getBody().get("output") instanceof Map);
    }

    @Test
    void prepare_withValidToken_returnsProfileFields() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("demo-token-123");

        Map<String, Object> body = Map.of(
                "appKey", "order-service",
                "code", "aiChat",
                "inputs", Map.of("question", "hello prepare")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/v1/llm/prepare", new HttpEntity<>(body, headers), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("profileVersion"));
        assertNotNull(response.getBody().get("gatewayBaseUrl"));
        assertNotNull(response.getBody().get("outboundSecretRef"));
    }

    @Test
    void invoke_withWrongToken_returnsAuthFailed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("wrong-token");

        Map<String, Object> body = Map.of(
                "appKey", "order-service",
                "code", "aiChat",
                "inputs", Map.of("question", "hello")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/v1/llm/invoke", new HttpEntity<>(body, headers), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FAILED", response.getBody().get("status"));
        assertEquals("AUTH_FAILED", response.getBody().get("errorCode"));
    }
}
