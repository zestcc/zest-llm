package cn.zest.www.zestllm.admin.controller.admin;

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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("it")
class AgentProfileProbeIT {

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
    void probePublished_persistsHistory() {
        String token = login();
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<Map> probeResp = restTemplate.exchange(
                "/api/admin/agent-profiles/aiChat/probe",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("smokeTest", false), headers),
                Map.class);
        assertEquals(HttpStatus.OK, probeResp.getStatusCode());
        Map<?, ?> probeData = unwrapData(probeResp.getBody());
        assertNotNull(probeData.get("overallStatus"));
        assertNotNull(probeData.get("probeId"));

        ResponseEntity<Map> historyResp = restTemplate.exchange(
                "/api/admin/agent-profiles/aiChat/probe/history?page=1&size=5&profileVersion=v1",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        assertEquals(HttpStatus.OK, historyResp.getStatusCode());
        Map<?, ?> historyData = unwrapData(historyResp.getBody());
        assertNotNull(historyData.get("records"));
        assertTrue(historyData.toString().contains("v1"));
    }

    private Map<?, ?> unwrapData(Map<?, ?> body) {
        if (body == null) {
            return Map.of();
        }
        Object data = body.get("data");
        if (data instanceof Map<?, ?> map) {
            return map;
        }
        return body;
    }

    private String login() {
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                "/api/admin/auth/login",
                Map.of("username", "admin", "password", "admin123"),
                Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        Object token = unwrapData(loginResp.getBody()).get("token");
        assertNotNull(token);
        return token.toString();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
