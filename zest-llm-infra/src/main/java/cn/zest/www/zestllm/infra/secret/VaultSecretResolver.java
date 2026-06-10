package cn.zest.www.zestllm.infra.secret;

import cn.zest.www.zestllm.infra.config.VaultProperties;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class VaultSecretResolver implements SecretResolver {

    private static final String PREFIX = "vault:";

    private final VaultProperties vaultProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public Optional<String> resolve(String secretRef) {
        if (secretRef == null || !secretRef.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String pathAndKey = secretRef.substring(PREFIX.length());
        if (pathAndKey.isBlank()) {
            return Optional.empty();
        }
        String path;
        String key = "value";
        int hash = pathAndKey.indexOf('#');
        if (hash >= 0) {
            path = pathAndKey.substring(0, hash);
            key = pathAndKey.substring(hash + 1);
        } else {
            path = pathAndKey;
        }
        if (vaultProperties.getToken() == null || vaultProperties.getToken().isBlank()) {
            log.warn("Vault token not configured, cannot resolve {}", secretRef);
            return Optional.empty();
        }
        try {
            String url = vaultProperties.getAddress().replaceAll("/$", "")
                    + "/v1/" + vaultProperties.getMount() + "/data/" + path.replaceAll("^/", "");
            String body = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("X-Vault-Token", vaultProperties.getToken())
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(body).path("data").path("data");
            if (data.has(key)) {
                return Optional.ofNullable(data.get(key).asText(null)).filter(v -> !v.isBlank());
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Vault resolve failed ref={}", secretRef, ex);
            return Optional.empty();
        }
    }
}
