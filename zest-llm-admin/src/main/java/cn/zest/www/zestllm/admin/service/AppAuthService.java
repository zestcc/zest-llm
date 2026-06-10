package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppAuthService {

    private final RuntimeAuthService runtimeAuthService;

    public LlmAppDO authenticate(String appKey, String bearerToken) {
        return runtimeAuthService.authenticate(appKey, bearerToken);
    }
}
