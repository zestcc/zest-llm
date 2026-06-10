package cn.zest.www.zestllm.spi.secret;

import java.util.Optional;

/**
 * Resolves secret references without exposing values through the control plane API.
 * Supported ref formats: env:VAR, vault:path#key, plain property key fallback.
 */
public interface SecretResolver {

    Optional<String> resolve(String secretRef);
}
