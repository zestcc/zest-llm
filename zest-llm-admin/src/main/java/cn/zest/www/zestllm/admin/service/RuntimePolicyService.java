package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.dto.ResolvedPolicy;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import cn.zest.www.zestllm.admin.util.FallbackModelsParser;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import cn.zest.www.zestllm.infra.cache.CaffeinePolicyCacheAdapter;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.cache.PolicyCacheAdapter;
import cn.zest.www.zestllm.spi.model.PromptTemplate;
import cn.zest.www.zestllm.spi.prompt.PromptRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuntimePolicyService {

    private static final Duration POLICY_CACHE_TTL = Duration.ofMinutes(5);

    private final RuntimeAuthService runtimeAuthService;
    private final AgentProfileResolver agentProfileResolver;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final PolicyCacheAdapter policyCacheAdapter;
    private final PromptRenderer promptRenderer;
    private final ObjectMapper objectMapper;

    public LlmAppDO authenticate(String appKey, String bearerToken) {
        return runtimeAuthService.authenticate(appKey, bearerToken);
    }

    public ResolvedPolicy resolvePolicy(LlmAppDO app, String code, Map<String, Object> inputs, String traceId) {
        Map<String, Object> variables = inputs != null ? inputs : Collections.emptyMap();
        String cacheKey = CaffeinePolicyCacheAdapter.buildKey(app.getAppKey(), code);
        Optional<CachedPolicy> cached = policyCacheAdapter.getPolicy(cacheKey);

        LlmAiTaskDefDO task;
        CachedPolicy policy;
        if (cached.isPresent() && cached.get().getTaskId() != null) {
            policy = cached.get();
            task = taskFromCachedPolicy(policy);
        } else {
            task = taskDefRepo.findByAppIdAndCode(app.getId(), code)
                    .orElseThrow(() -> new ZestLlmException(LlmErrorCode.TASK_NOT_FOUND, traceId));
            policy = cached.orElseGet(() -> loadAndCachePolicy(app, task, cacheKey, traceId));
        }
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
        CachedPolicy policy = agentProfileResolver.resolve(app, task, traceId);
        policyCacheAdapter.putPolicy(cacheKey, policy, POLICY_CACHE_TTL);
        return policy;
    }

    private static LlmAiTaskDefDO taskFromCachedPolicy(CachedPolicy policy) {
        LlmAiTaskDefDO task = new LlmAiTaskDefDO();
        task.setId(policy.getTaskId());
        task.setCode(policy.getTaskCode());
        return task;
    }

    public List<String> parseFallbackModels(String fallbackModels) {
        return FallbackModelsParser.parse(fallbackModels, objectMapper);
    }
}
