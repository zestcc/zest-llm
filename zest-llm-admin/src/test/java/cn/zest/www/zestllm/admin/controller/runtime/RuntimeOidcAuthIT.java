package cn.zest.www.zestllm.admin.controller.runtime;

import cn.zest.www.zestllm.admin.model.request.UpsertAuthBindingRequest;
import cn.zest.www.zestllm.admin.service.AuthBindingManageService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("it")
class RuntimeOidcAuthIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("zest_llm")
            .withUsername("test")
            .withPassword("test");

    static WireMockServer litellmMock;
    static WireMockServer jwksMock;
    static KeyPair keyPair;
    static String jwksUri;

    static {
        litellmMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        litellmMock.start();
        litellmMock.stubFor(get(urlEqualTo("/health/liveliness")).willReturn(aResponse().withStatus(200)));
        litellmMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"choices":[{"message":{"content":"{\\"answer\\":\\"oidc-ok\\"}"}}],
                                 "usage":{"prompt_tokens":1,"completion_tokens":1}}
                                """)));

        jwksMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        jwksMock.start();
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            String n = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray());
            String e = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray());
            jwksUri = "http://localhost:" + jwksMock.port() + "/jwks.json";
            jwksMock.stubFor(get(urlEqualTo("/jwks.json"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"keys":[{"kty":"RSA","kid":"test-key","n":"%s","e":"%s","use":"sig","alg":"RS256"}]}
                                    """.formatted(n, e))));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private AuthBindingManageService authBindingManageService;

    @AfterAll
    static void stopMocks() {
        if (litellmMock != null) {
            litellmMock.stop();
        }
        if (jwksMock != null) {
            jwksMock.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("zest.llm.litellm.base-url", () -> "http://localhost:" + litellmMock.port());
    }

    @Test
    void invoke_withOidcJwt_returnsSuccess() {
        UpsertAuthBindingRequest binding = new UpsertAuthBindingRequest();
        binding.setScopeType("APP");
        binding.setAppKey("order-service");
        binding.setInboundMode("OIDC_JWT");
        binding.setInboundConfigJson("""
                {"mode":"OIDC_JWT","issuer":"https://test-issuer","audience":"zest-llm","jwksUri":"%s"}
                """.formatted(jwksUri));
        authBindingManageService.upsert(binding);

        String jwt = Jwts.builder()
                .header().keyId("test-key").and()
                .issuer("https://test-issuer")
                .subject("user-oidc-it")
                .audience().add("zest-llm").and()
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt);

        Map<String, Object> body = Map.of(
                "appKey", "order-service",
                "code", "aiChat",
                "inputs", Map.of("question", "oidc it")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/v1/llm/invoke", new HttpEntity<>(body, headers), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertNotNull(response.getBody().get("traceId"));
    }
}
