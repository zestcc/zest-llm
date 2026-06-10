package cn.zest.www.zestllm.infra.secret;

import cn.zest.www.zestllm.spi.secret.SecretResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EnvSecretResolver implements SecretResolver {

    private static final String PREFIX = "env:";

    @Override
    public Optional<String> resolve(String secretRef) {
        if (secretRef == null || !secretRef.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String envName = secretRef.substring(PREFIX.length());
        if (envName.isBlank()) {
            return Optional.empty();
        }
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envName);
        }
        return Optional.ofNullable(value).filter(v -> !v.isBlank());
    }
}
