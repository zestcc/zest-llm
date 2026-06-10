package cn.zest.www.zestllm.infra.secret;

import cn.zest.www.zestllm.spi.secret.SecretResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Primary
@Component
public class CompositeSecretResolver implements SecretResolver {

    private final List<SecretResolver> resolvers;

    public CompositeSecretResolver(List<SecretResolver> resolvers) {
        this.resolvers = resolvers.stream()
                .filter(r -> !(r instanceof CompositeSecretResolver))
                .toList();
    }

    @Override
    public Optional<String> resolve(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            return Optional.empty();
        }
        for (SecretResolver resolver : resolvers) {
            Optional<String> value = resolver.resolve(secretRef);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }
}
