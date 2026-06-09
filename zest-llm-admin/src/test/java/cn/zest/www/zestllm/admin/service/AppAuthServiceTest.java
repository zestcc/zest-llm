package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.util.TokenHashUtil;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppAuthServiceTest {

    @Mock
    private LlmAppRepo appRepo;

    @InjectMocks
    private AppAuthService appAuthService;

    @Test
    void authenticate_rejectsWrongToken() {
        LlmAppDO app = new LlmAppDO();
        app.setAppKey("order-service");
        app.setStatus("ACTIVE");
        app.setTokenHash(TokenHashUtil.sha256Hex("demo-token-123"));
        when(appRepo.findByAppKey("order-service")).thenReturn(Optional.of(app));

        ZestLlmException ex = assertThrows(ZestLlmException.class,
                () -> appAuthService.authenticate("order-service", "wrong-token"));
        assertEquals(LlmErrorCode.AUTH_FAILED, ex.getErrorCode());
    }

    @Test
    void authenticate_acceptsValidToken() {
        LlmAppDO app = new LlmAppDO();
        app.setAppKey("order-service");
        app.setStatus("ACTIVE");
        app.setTokenHash(TokenHashUtil.sha256Hex("demo-token-123"));
        when(appRepo.findByAppKey("order-service")).thenReturn(Optional.of(app));

        LlmAppDO result = appAuthService.authenticate("order-service", "demo-token-123");
        assertEquals("order-service", result.getAppKey());
    }
}
