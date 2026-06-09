package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.dto.ResolvedPolicy;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.infra.cache.CaffeinePolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.model.PromptTemplate;
import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimePolicyService {

    private static final Duration POLICY_CACHE_TTL = Duration.ofMinutes(5);

    private final AppAuthService appAuthService;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmPromptVersionRepo promptVersionRepo;
    private final LlmModelRouteRepo modelRouteRepo;
    private final PolicyCacheAdapter policyCacheAdapter;
    private final PromptRenderer promptRenderer;
    private final ObjectMapper objectMapper;

    public LlmAppDO authenticate(String appKey, String bearerToken) {
        return appAuthService.authenticate(appKey, bearerToken);
    }

    public ResolvedPolicy resolvePolicy(LlmAppDO app, String code, Map<String, Object> inputs, String traceId) {
        LlmAiTaskDefDO task = taskDefRepo.findByAppIdAndCode(app.getId(), code)
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.TASK_NOT_FOUND, traceId));

        Map<String, Object> variables = inputs != null ? inputs : Collections.emptyMap();
        String cacheKey = CaffeinePolicyCacheAdapter.buildKey(app.getAppKey(), code);
        Optional<CachedPolicy> cached = policyCacheAdapter.getPolicy(cacheKey);
        CachedPolicy policy = cached.orElseGet(() -> loadAndCachePolicy(app, task, cacheKey, traceId));
        String rendered = promptRenderer.render(PromptTemplate.builder()
                .templateBody(policy.getTemplateBody())
                .version(policy.getPromptVersion())
                .build(), variables);

        return ResolvedPolicy.builder()
                .app(app)
                .task(task)
                .policy(policy)
                .renderedPrompt(rendered)
                .build();
    }

    private CachedPolicy loadAndCachePolicy(LlmAppDO app, LlmAiTaskDefDO task, String cacheKey, String traceId) {
        LlmPromptVersionDO prompt = promptVersionRepo.findPublishedByTaskId(task.getId())
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.PROMPT_NOT_FOUND, traceId));
        LlmModelRouteDO route = modelRouteRepo.findActiveByTaskId(task.getId())
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.INTERNAL_ERROR, traceId, "模型路由未配置"));

        CachedPolicy policy = CachedPolicy.builder()
                .promptVersion(prompt.getVersion())
                .templateBody(prompt.getTemplateBody())
                .outputSchema(prompt.getOutputSchema())
                .primaryModel(route.getPrimaryModel())
                .fallbackModels(parseFallbackModels(route.getFallbackModels()))
                .maxTokens(route.getMaxTokens())
                .temperature(route.getTemperature() != null ? route.getTemperature().doubleValue() : null)
                .timeoutMs(route.getTimeoutMs())
                .build();
        policyCacheAdapter.putPolicy(cacheKey, policy, POLICY_CACHE_TTL);
        return policy;
    }

    public List<String> parseFallbackModels(String fallbackModels) {
        if (fallbackModels == null || fallbackModels.isBlank()) {
            return List.of();
        }
        if (fallbackModels.startsWith("[")) {
            try {
                return objectMapper.readValue(fallbackModels, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException ex) {
                log.warn("Invalid fallback_models JSON: {}", fallbackModels);
            }
        }
        return Arrays.stream(fallbackModels.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
