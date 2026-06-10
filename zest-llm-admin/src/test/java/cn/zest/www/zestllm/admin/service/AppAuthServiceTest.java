package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppAuthServiceTest {

    @Mock
    private RuntimeAuthService runtimeAuthService;

    @InjectMocks
    private AppAuthService appAuthService;

    @Test
    void authenticate_delegatesToRuntimeAuthService() {
        appAuthService.authenticate("order-service", "demo-token-123");
        verify(runtimeAuthService).authenticate("order-service", "demo-token-123");
    }
}
