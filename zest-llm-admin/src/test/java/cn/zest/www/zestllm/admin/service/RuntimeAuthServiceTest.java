package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmAuthBindingRepo;
import cn.zest.www.zestllm.admin.service.auth.RuntimeAuthService;
import cn.zest.www.zestllm.admin.service.auth.StaticTokenAuthStrategy;
import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimeAuthServiceTest {

    @Mock
    private LlmAppRepo appRepo;
    @Mock
    private LlmAuthBindingRepo authBindingRepo;

    private RuntimeAuthService runtimeAuthService;

    @BeforeEach
    void setUp() {
        runtimeAuthService = new RuntimeAuthService(
                appRepo,
                authBindingRepo,
                new ObjectMapper(),
                List.of(new StaticTokenAuthStrategy()));
    }

    @Test
    void authenticate_rejectsWrongToken() {
        LlmAppDO app = app("order-service", "demo-token-123");
        when(appRepo.findByAppKey("order-service")).thenReturn(Optional.of(app));
        when(authBindingRepo.findByScope("APP", app.getId())).thenReturn(Optional.empty());

        ZestLlmException ex = assertThrows(ZestLlmException.class,
                () -> runtimeAuthService.authenticate("order-service", "wrong-token"));
        assertEquals(LlmErrorCode.AUTH_FAILED, ex.getErrorCode());
    }

    @Test
    void authenticate_acceptsValidToken() {
        LlmAppDO app = app("order-service", "demo-token-123");
        when(appRepo.findByAppKey("order-service")).thenReturn(Optional.of(app));
        when(authBindingRepo.findByScope("APP", app.getId())).thenReturn(Optional.empty());

        LlmAppDO result = runtimeAuthService.authenticate("order-service", "demo-token-123");
        assertEquals("order-service", result.getAppKey());
    }

    private LlmAppDO app(String appKey, String rawToken) {
        LlmAppDO app = new LlmAppDO();
        app.setId(1L);
        app.setAppKey(appKey);
        app.setStatus("ACTIVE");
        app.setAuthMode("STATIC_TOKEN");
        app.setTokenHash(TokenHashUtil.sha256Hex(rawToken));
        return app;
    }
}
