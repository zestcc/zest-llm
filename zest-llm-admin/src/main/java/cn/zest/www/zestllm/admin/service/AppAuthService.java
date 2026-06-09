package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppAuthService {

    private final LlmAppRepo appRepo;

    public LlmAppDO authenticate(String appKey, String bearerToken) {
        if (appKey == null || appKey.isBlank()) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        LlmAppDO app = appRepo.findByAppKey(appKey)
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.AUTH_FAILED));
        if (!"ACTIVE".equals(app.getStatus())) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        if (!TokenHashUtil.matches(bearerToken, app.getTokenHash())) {
            throw new ZestLlmException(LlmErrorCode.AUTH_FAILED);
        }
        return app;
    }
}
