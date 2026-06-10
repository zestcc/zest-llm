package cn.zest.www.zestllm.admin.service.auth;

import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OidcJwtValidatorTest {

    private static WireMockServer wireMock;
    private static KeyPair keyPair;
    private static String jwksUri;
    private static OidcJwtValidator validator;

    @BeforeAll
    static void setUp() throws Exception {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        jwksUri = "http://localhost:" + wireMock.port() + "/jwks.json";

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray());

        wireMock.stubFor(get(urlEqualTo("/jwks.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"keys":[{"kty":"RSA","kid":"test-key","n":"%s","e":"%s","use":"sig","alg":"RS256"}]}
                                """.formatted(n, e))));

        validator = new OidcJwtValidator(new ObjectMapper(), RestClient.builder());
    }

    @AfterAll
    static void tearDown() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void validate_acceptsSignedJwt() {
        String jwt = Jwts.builder()
                .header().keyId("test-key").and()
                .issuer("https://test-issuer")
                .subject("user-1")
                .audience().add("zest-llm").and()
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        InboundAuthConfig config = new InboundAuthConfig();
        config.setIssuer("https://test-issuer");
        config.setAudience("zest-llm");
        config.setJwksUri(jwksUri);

        assertDoesNotThrow(() -> validator.validate(jwt, config));
    }

    @Test
    void validate_rejectsExpiredJwt() {
        String jwt = Jwts.builder()
                .header().keyId("test-key").and()
                .issuer("https://test-issuer")
                .audience().add("zest-llm").and()
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        InboundAuthConfig config = new InboundAuthConfig();
        config.setIssuer("https://test-issuer");
        config.setAudience("zest-llm");
        config.setJwksUri(jwksUri);

        ZestLlmException ex = assertThrows(ZestLlmException.class, () -> validator.validate(jwt, config));
        assertEquals(LlmErrorCode.AUTH_FAILED, ex.getErrorCode());
    }
}
