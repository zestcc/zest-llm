package cn.zest.www.zestllm.admin.service.auth;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAuthBindingDO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmAuthBindingRepo;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.spi.profile.InboundAuthConfig;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthContext;
import cn.zest.www.zestllm.spi.auth.RuntimeAuthStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuntimeAuthService {

    private final LlmAppRepo appRepo;
    private final LlmAuthBindingRepo authBindingRepo;
    private final ObjectMapper objectMapper;
    private final Map<String, RuntimeAuthStrategy> strategies;

    public RuntimeAuthService(LlmAppRepo appRepo,
                              LlmAuthBindingRepo authBindingRepo,
                              ObjectMapper objectMapper,
                              List<RuntimeAuthStrategy> strategyList) {
        this.appRepo = appRepo;
        this.authBindingRepo = authBindingRepo;
        this.objectMapper = objectMapper;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(RuntimeAuthStrategy::mode, Function.identity(), (a, b) -> a));
    }

    public LlmAppDO authenticate(String appKey, String bearerToken) {
        if (appKey == null || appKey.isBlank()) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        LlmAppDO app = appRepo.findByAppKey(appKey)
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.AUTH_FAILED));
        if (!"ACTIVE".equals(app.getStatus())) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        InboundAuthConfig inboundAuth = resolveInboundAuth(app);
        String mode = inboundAuth.getMode() != null ? inboundAuth.getMode() : StaticTokenAuthStrategy.MODE;
        RuntimeAuthStrategy strategy = strategies.getOrDefault(mode, strategies.get(StaticTokenAuthStrategy.MODE));
        strategy.authenticate(RuntimeAuthContext.builder()
                .appKey(appKey)
                .bearerToken(bearerToken)
                .tokenHash(app.getTokenHash())
                .inboundAuth(inboundAuth)
                .build());
        return app;
    }

    public InboundAuthConfig resolveInboundAuth(LlmAppDO app) {
        Optional<LlmAuthBindingDO> binding = authBindingRepo.findByScope("APP", app.getId());
        if (binding.isPresent()) {
            InboundAuthConfig fromBinding = parseInbound(binding.get());
            if (fromBinding != null) {
                return fromBinding;
            }
        }
        if (StringUtils.hasText(app.getAuthConfigJson())) {
            try {
                InboundAuthConfig parsed = objectMapper.readValue(app.getAuthConfigJson(), InboundAuthConfig.class);
                if (parsed.getMode() == null && StringUtils.hasText(app.getAuthMode())) {
                    parsed.setMode(app.getAuthMode());
                }
                return parsed;
            } catch (Exception ex) {
                log.warn("Invalid auth_config_json for app {}", app.getAppKey());
            }
        }
        InboundAuthConfig fallback = new InboundAuthConfig();
        fallback.setMode(StringUtils.hasText(app.getAuthMode()) ? app.getAuthMode() : StaticTokenAuthStrategy.MODE);
        return fallback;
    }

    private InboundAuthConfig parseInbound(LlmAuthBindingDO binding) {
        if (!StringUtils.hasText(binding.getInboundConfigJson())) {
            InboundAuthConfig config = new InboundAuthConfig();
            config.setMode(binding.getInboundMode());
            return config;
        }
        try {
            return objectMapper.readValue(binding.getInboundConfigJson(), InboundAuthConfig.class);
        } catch (Exception ex) {
            log.warn("Invalid inbound_config_json bindingId={}", binding.getId());
            return null;
        }
    }
}
